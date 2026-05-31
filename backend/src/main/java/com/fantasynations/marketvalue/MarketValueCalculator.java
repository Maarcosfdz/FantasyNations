package com.fantasynations.marketvalue;

import com.fantasynations.domain.AvailabilityStatus;
import com.fantasynations.domain.Importance;
import com.fantasynations.domain.LeagueReputation;
import com.fantasynations.domain.MarketValueChangeReason;
import com.fantasynations.domain.NationalTeamTier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure, deterministic calculator. No database, no services. The same input
 * always produces the same output, so it is trivial to unit-test.
 *
 * Two entry points:
 *   - calculateInitial(input)
 *   - calculateDelta(input)
 */
@Component
public class MarketValueCalculator {

    private final MarketValueConfig cfg;
    private final NationalTeamTierResolver tierResolver;

    public MarketValueCalculator(MarketValueConfig cfg, NationalTeamTierResolver tierResolver) {
        this.cfg = cfg;
        this.tierResolver = tierResolver;
    }

    // ---------------------------------------------------------------- INITIAL

    public InitialValueResult calculateInitial(InitialValueInput in) {
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();

        BigDecimal posBase = cfg.positionBaseValue.get(in.position());
        breakdown.put("positionBase", posBase);

        NationalTeamTier tier = tierResolver.resolve(in.nationalTeam());
        BigDecimal tierBonus = cfg.tierBonus.get(tier);
        breakdown.put("tier", BigDecimal.valueOf(tier.ordinal()));
        breakdown.put("tierBonus", tierBonus);

        Importance importance = in.importance(); // may be null
        BigDecimal impBonus = importance == null
                ? BigDecimal.ZERO
                : cfg.importanceBonus.get(importance);
        breakdown.put("importanceBonus", impBonus);

        LeagueReputation rep = in.leagueReputation() == null ? LeagueReputation.LOW : in.leagueReputation();
        BigDecimal repBonus = cfg.leagueReputationBonus.get(rep);
        breakdown.put("leagueReputationBonus", repBonus);

        BigDecimal manual = in.manualStarBonus() == null ? BigDecimal.ZERO : in.manualStarBonus();
        breakdown.put("manualStarBonus", manual);

        BigDecimal raw = posBase.add(tierBonus).add(impBonus).add(repBonus).add(manual);
        breakdown.put("rawSum", raw);

        BigDecimal chosen = in.initialValueOverride() != null ? in.initialValueOverride() : raw;
        breakdown.put("preBoundsAndRounding", chosen);

        BigDecimal bounded = clamp(chosen, cfg.initialMin, cfg.initialMax);
        BigDecimal rounded = roundToUnit(bounded, cfg.roundingUnit);
        breakdown.put("final", rounded);

        return new InitialValueResult(rounded, breakdown);
    }

    // ---------------------------------------------------------------- DYNAMIC

    public MarketValueResult calculateDelta(DynamicValueInput in) {
        Map<String, Object> breakdown = new LinkedHashMap<>();
        BigDecimal oldValue = in.currentMarketValue();

        int pointsScore = pointsScore(in.lastMatchdayPoints()) + avgPointsModifier(in.averagePoints());
        int minutesScore = minutesScore(in.minutesPlayed(), in.didNotPlay());
        int demandScore  = in.demandScore() == null ? 0 : in.demandScore();
        int availabilityScore = cfg.availabilityScore.get(safeAvailability(in.availability()));
        int tournamentScore = tournamentScore(in);

        int momentum = pointsScore + minutesScore + demandScore + availabilityScore + tournamentScore;

        breakdown.put("pointsScore", pointsScore);
        breakdown.put("minutesScore", minutesScore);
        breakdown.put("demandScore", demandScore);
        breakdown.put("availabilityScore", availabilityScore);
        breakdown.put("tournamentScore", tournamentScore);
        breakdown.put("momentumScore", momentum);

        BigDecimal basePercent = momentumToPercent(momentum);
        BigDecimal percent = applySpecialRules(in, basePercent, breakdown);

        // Hard caps.
        percent = percent.max(cfg.maxDecreasePercentPerCycle).min(cfg.maxIncreasePercentPerCycle);
        breakdown.put("percentAfterCaps", percent);

        BigDecimal rawNew = oldValue.add(oldValue.multiply(percent)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        BigDecimal bounded = clamp(rawNew, cfg.futureMin, cfg.futureMax);
        BigDecimal newValue = roundToUnit(bounded, cfg.roundingUnit);
        BigDecimal delta = newValue.subtract(oldValue);

        MarketValueChangeReason reason = inferReason(in, percent, delta);
        breakdown.put("reason", reason.name());
        breakdown.put("newValue", newValue);

        return new MarketValueResult(oldValue, newValue, delta, percent, momentum, reason, breakdown);
    }

    // ---------------------------------------------------------------- helpers

    private int pointsScore(Integer last) {
        if (last == null) return 0;
        if (last < 0) return cfg.pointsScoreNegative;
        if (last == 0) return cfg.pointsScoreZero;
        for (int[] row : cfg.pointsScoreTable) {
            if (last >= row[0]) return row[1];
        }
        return 0;
    }

    private int avgPointsModifier(Double avg) {
        if (avg == null) return 0;
        if (avg < 2) return cfg.avgPointsBelowTwoModifier;
        for (int[] row : cfg.avgPointsModifierTable) {
            if (avg >= row[0]) return row[1];
        }
        return 0;
    }

    private int minutesScore(Integer minutes, boolean dnp) {
        if (dnp) return cfg.minutesScoreDidNotPlay;
        if (minutes == null) return 0;
        if (minutes >= 90) return cfg.minutesScore90Plus;
        if (minutes >= 60) return cfg.minutesScore60To89;
        if (minutes >= 1)  return cfg.minutesScore1To59;
        return cfg.minutesScoreDidNotPlay;
    }

    private AvailabilityStatus safeAvailability(AvailabilityStatus a) {
        return a == null ? AvailabilityStatus.AVAILABLE : a;
    }

    private int tournamentScore(DynamicValueInput in) {
        if (in.teamReachedFinal())      return cfg.tournamentFinal;
        if (in.teamReachedSemiFinals()) return cfg.tournamentSemiFinals;
        if (in.teamEliminated())        return cfg.tournamentEliminated;
        return cfg.tournamentStillAlive;
    }

    private BigDecimal momentumToPercent(int score) {
        BigDecimal result = BigDecimal.ZERO;
        // Walk ascending; the last band whose min <= score wins.
        for (int[] band : cfg.momentumToPercent) {
            if (score >= band[0]) result = BigDecimal.valueOf(band[1]);
        }
        // Spec: score < -6 -> -12% (the first band has min = -7 so anything
        // smaller still falls under it).
        if (score < cfg.momentumToPercent[0][0]) {
            result = BigDecimal.valueOf(cfg.momentumToPercent[0][1]);
        }
        return result;
    }

    /**
     * Applies the spec's "special rules" floors. Each special rule sets a
     * MINIMUM penalty - the percent can be more negative, never less.
     */
    private BigDecimal applySpecialRules(DynamicValueInput in,
                                         BigDecimal percent,
                                         Map<String, Object> breakdown) {
        BigDecimal p = percent;
        boolean played60Positive = in.minutesPlayed() != null
                && in.minutesPlayed() >= 60
                && in.lastMatchdayPoints() != null
                && in.lastMatchdayPoints() > 0;

        AvailabilityStatus avail = safeAvailability(in.availability());
        boolean injured   = avail == AvailabilityStatus.INJURED;
        boolean suspended = avail == AvailabilityStatus.SUSPENDED;

        // Strong-performance protection (only if not knocked out by hard floors below).
        if (played60Positive && !injured && !suspended && !in.teamEliminated()) {
            p = p.max(BigDecimal.ZERO);
            breakdown.put("strongPerformanceProtected", true);
        }

        if (injured) {
            // At least the injury penalty, and never positive.
            p = p.min(cfg.injuredMinPenaltyPercent);
            breakdown.put("injuredFloorApplied", true);
        }
        if (suspended) {
            p = p.min(cfg.suspendedMinPenaltyPercent);
            breakdown.put("suspendedFloorApplied", true);
        }
        if (in.teamEliminated()) {
            p = p.min(cfg.eliminatedMinPenaltyPercent);
            breakdown.put("eliminatedFloorApplied", true);
        }
        if (in.didNotPlay() && !in.teamEliminated()) {
            boolean allowZero = in.restedSuperstar()
                    && (in.importance() == Importance.GLOBAL_SUPERSTAR
                            || in.importance() == Importance.STAR);
            if (!allowZero) {
                p = p.min(cfg.didNotPlayMinPenaltyPercent);
                breakdown.put("didNotPlayFloorApplied", true);
            } else {
                p = p.max(BigDecimal.ZERO).min(BigDecimal.ZERO);
                breakdown.put("restedSuperstarException", true);
            }
        }
        return p;
    }

    private MarketValueChangeReason inferReason(DynamicValueInput in,
                                                BigDecimal percent,
                                                BigDecimal delta) {
        AvailabilityStatus avail = safeAvailability(in.availability());
        if (avail == AvailabilityStatus.INJURED)   return MarketValueChangeReason.INJURED;
        if (avail == AvailabilityStatus.SUSPENDED) return MarketValueChangeReason.SUSPENDED;
        if (in.teamEliminated())                   return MarketValueChangeReason.TEAM_ELIMINATED;
        if (in.didNotPlay())                       return MarketValueChangeReason.DID_NOT_PLAY;
        int cmp = delta.signum();
        if (cmp > 0) return MarketValueChangeReason.HIGH_PERFORMANCE;
        if (cmp < 0) return MarketValueChangeReason.LOW_PERFORMANCE;
        return MarketValueChangeReason.STABLE;
    }

    private static BigDecimal clamp(BigDecimal v, BigDecimal lo, BigDecimal hi) {
        return v.max(lo).min(hi);
    }

    /** Rounds half-up to the nearest multiple of {@code unit}. */
    static BigDecimal roundToUnit(BigDecimal v, BigDecimal unit) {
        if (unit.signum() == 0) return v;
        BigDecimal[] qr = v.divideAndRemainder(unit);
        BigDecimal floor = qr[0].multiply(unit);
        BigDecimal halfUnit = unit.divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
        if (qr[1].abs().compareTo(halfUnit) >= 0) {
            return v.signum() >= 0 ? floor.add(unit) : floor.subtract(unit);
        }
        return floor.setScale(0, RoundingMode.UNNECESSARY);
    }
}
