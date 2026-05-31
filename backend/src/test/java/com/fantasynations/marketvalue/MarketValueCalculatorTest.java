package com.fantasynations.marketvalue;

import com.fantasynations.domain.AvailabilityStatus;
import com.fantasynations.domain.Importance;
import com.fantasynations.domain.LeagueReputation;
import com.fantasynations.domain.MarketValueChangeReason;
import com.fantasynations.domain.PlayerPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MarketValueCalculatorTest {

    private MarketValueConfig cfg;
    private MarketValueCalculator calc;

    @BeforeEach
    void setUp() {
        cfg = new MarketValueConfig();
        calc = new MarketValueCalculator(cfg, new NationalTeamTierResolver(cfg));
    }

    // ----------------------------------------------------------- initial value

    @Test
    void positionBaseValuesAreApplied() {
        // Costa Rica is tier C (zero), no importance, no reputation -> only position base.
        for (PlayerPosition p : PlayerPosition.values()) {
            var input = InitialValueInput.builder()
                    .position(p).nationalTeam("Costa Rica").build();
            BigDecimal v = calc.calculateInitial(input).value();
            assertThat(v).isEqualByComparingTo(cfg.positionBaseValue.get(p));
        }
    }

    @Test
    void tierBonusIsApplied() {
        var input = InitialValueInput.builder()
                .position(PlayerPosition.MID).nationalTeam("Argentina").build(); // S tier
        BigDecimal v = calc.calculateInitial(input).value();
        assertThat(v).isEqualByComparingTo(new BigDecimal("18000000")); // 8M base + 10M S
    }

    @Test
    void importanceBonusIsApplied() {
        var input = InitialValueInput.builder()
                .position(PlayerPosition.FWD)
                .nationalTeam("Costa Rica")
                .importance(Importance.STAR)
                .build();
        // 10M + 0 + 30M = 40M
        assertThat(calc.calculateInitial(input).value())
                .isEqualByComparingTo(new BigDecimal("40000000"));
    }

    @Test
    void missingLeagueReputationFallsBackToLow() {
        var input = InitialValueInput.builder()
                .position(PlayerPosition.GK).nationalTeam("Costa Rica")
                .leagueReputation(null).build();
        assertThat(calc.calculateInitial(input).value())
                .isEqualByComparingTo(new BigDecimal("4000000"));
    }

    @Test
    void leagueReputationBonusIsApplied() {
        var input = InitialValueInput.builder()
                .position(PlayerPosition.GK).nationalTeam("Costa Rica")
                .leagueReputation(LeagueReputation.ELITE).build();
        assertThat(calc.calculateInitial(input).value())
                .isEqualByComparingTo(new BigDecimal("14000000")); // 4M + 10M
    }

    @Test
    void manualOverrideReplacesComputedValue() {
        var input = InitialValueInput.builder()
                .position(PlayerPosition.FWD).nationalTeam("Argentina")
                .importance(Importance.GLOBAL_SUPERSTAR)
                .leagueReputation(LeagueReputation.ELITE)
                .initialValueOverride(new BigDecimal("70000000"))
                .build();
        assertThat(calc.calculateInitial(input).value())
                .isEqualByComparingTo(new BigDecimal("70000000"));
    }

    @Test
    void maxInitialCapIs70Million() {
        // Sum would exceed 70M: 10M (FWD) + 10M (S) + 40M (Superstar) + 10M (Elite) = 70M.
        // Bump it with a manual star bonus so the cap actually applies.
        var input = InitialValueInput.builder()
                .position(PlayerPosition.FWD).nationalTeam("Argentina")
                .importance(Importance.GLOBAL_SUPERSTAR)
                .leagueReputation(LeagueReputation.ELITE)
                .manualStarBonus(new BigDecimal("20000000"))
                .build();
        assertThat(calc.calculateInitial(input).value())
                .isEqualByComparingTo(new BigDecimal("70000000"));
    }

    @Test
    void minInitialFloorIs1Million() {
        var input = InitialValueInput.builder()
                .position(PlayerPosition.GK).nationalTeam("Qatar")
                .initialValueOverride(new BigDecimal("100"))
                .build();
        assertThat(calc.calculateInitial(input).value())
                .isEqualByComparingTo(new BigDecimal("1000000"));
    }

    @Test
    void roundsToNearestHundredThousand() {
        var input = InitialValueInput.builder()
                .position(PlayerPosition.MID).nationalTeam("Costa Rica")
                .initialValueOverride(new BigDecimal("8049999"))
                .build();
        assertThat(calc.calculateInitial(input).value())
                .isEqualByComparingTo(new BigDecimal("8000000"));

        var input2 = InitialValueInput.builder()
                .position(PlayerPosition.MID).nationalTeam("Costa Rica")
                .initialValueOverride(new BigDecimal("8050000"))
                .build();
        assertThat(calc.calculateInitial(input2).value())
                .isEqualByComparingTo(new BigDecimal("8100000"));
    }

    // ----------------------------------------------------------- dynamic value

    private DynamicValueInput.Builder baseDyn() {
        return DynamicValueInput.builder()
                .currentMarketValue(new BigDecimal("20000000"))
                .importance(Importance.STARTER)
                .availability(AvailabilityStatus.AVAILABLE)
                .minutesPlayed(90)
                .lastMatchdayPoints(0);
    }

    @Test
    void highPointsIncreaseValue() {
        var in = baseDyn().lastMatchdayPoints(16).averagePoints(8.0).build();
        var r = calc.calculateDelta(in);
        // 8 (>=15) + 3 (avg>=8) = 11 points-score, minutes=2, avail=0 -> momentum 13 -> +10%
        assertThat(r.newValue()).isGreaterThan(r.oldValue());
        assertThat(r.reason()).isEqualTo(MarketValueChangeReason.HIGH_PERFORMANCE);
        assertThat(r.deltaPercent()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    void averagePointsSmallIncreaseRespectsCaps() {
        var in = baseDyn().lastMatchdayPoints(2).averagePoints(5.0).build();
        var r = calc.calculateDelta(in);
        // 1 + 1 + 2 = 4 -> +4%
        assertThat(r.deltaPercent()).isEqualByComparingTo(new BigDecimal("4"));
    }

    @Test
    void zeroPointsDecreaseValue() {
        var in = baseDyn().lastMatchdayPoints(0).minutesPlayed(70).averagePoints(1.0).build();
        var r = calc.calculateDelta(in);
        // points=-1, avgmod=-1, minutes=1 -> momentum -1 -> -3%
        assertThat(r.deltaPercent()).isEqualByComparingTo(new BigDecimal("-3"));
        assertThat(r.reason()).isEqualTo(MarketValueChangeReason.LOW_PERFORMANCE);
    }

    @Test
    void negativePointsDecreaseValue() {
        var in = baseDyn().lastMatchdayPoints(-2).minutesPlayed(90).averagePoints(1.0).build();
        var r = calc.calculateDelta(in);
        assertThat(r.deltaPercent().signum()).isLessThan(0);
    }

    @Test
    void positiveAndSixtyMinutesDoesNotDecrease() {
        var in = baseDyn()
                .lastMatchdayPoints(5).averagePoints(1.0)
                .minutesPlayed(70).build();
        var r = calc.calculateDelta(in);
        assertThat(r.newValue()).isGreaterThanOrEqualTo(r.oldValue());
    }

    @Test
    void didNotPlayDecreasesAtLeast5Percent() {
        var in = baseDyn().didNotPlay(true).lastMatchdayPoints(null).build();
        var r = calc.calculateDelta(in);
        assertThat(r.deltaPercent()).isLessThanOrEqualTo(new BigDecimal("-5"));
        assertThat(r.reason()).isEqualTo(MarketValueChangeReason.DID_NOT_PLAY);
    }

    @Test
    void restedSuperstarMayKeepZero() {
        var in = baseDyn()
                .importance(Importance.GLOBAL_SUPERSTAR)
                .didNotPlay(true).restedSuperstar(true)
                .lastMatchdayPoints(null).build();
        var r = calc.calculateDelta(in);
        assertThat(r.deltaPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void injuredDecreasesAtLeast8Percent() {
        var in = baseDyn()
                .availability(AvailabilityStatus.INJURED)
                .lastMatchdayPoints(8).averagePoints(8.0)
                .build();
        var r = calc.calculateDelta(in);
        assertThat(r.deltaPercent()).isLessThanOrEqualTo(new BigDecimal("-8"));
        assertThat(r.reason()).isEqualTo(MarketValueChangeReason.INJURED);
    }

    @Test
    void suspendedDecreasesAtLeast5Percent() {
        var in = baseDyn()
                .availability(AvailabilityStatus.SUSPENDED)
                .lastMatchdayPoints(null).minutesPlayed(null)
                .build();
        var r = calc.calculateDelta(in);
        assertThat(r.deltaPercent()).isLessThanOrEqualTo(new BigDecimal("-5"));
        assertThat(r.reason()).isEqualTo(MarketValueChangeReason.SUSPENDED);
    }

    @Test
    void eliminatedDecreasesAtLeast10Percent() {
        var in = baseDyn().teamEliminated(true).build();
        var r = calc.calculateDelta(in);
        assertThat(r.deltaPercent()).isLessThanOrEqualTo(new BigDecimal("-10"));
        assertThat(r.reason()).isEqualTo(MarketValueChangeReason.TEAM_ELIMINATED);
    }

    @Test
    void increaseCappedAt15Percent() {
        var in = baseDyn()
                .lastMatchdayPoints(50).averagePoints(20.0)
                .demandScore(20).teamReachedFinal(true).build();
        var r = calc.calculateDelta(in);
        assertThat(r.deltaPercent()).isLessThanOrEqualTo(new BigDecimal("15"));
    }

    @Test
    void decreaseCappedAt15Percent() {
        var in = baseDyn()
                .availability(AvailabilityStatus.OUT_OF_TOURNAMENT)
                .lastMatchdayPoints(-10).averagePoints(0.0)
                .didNotPlay(true).teamEliminated(true).demandScore(-10).build();
        var r = calc.calculateDelta(in);
        assertThat(r.deltaPercent()).isGreaterThanOrEqualTo(new BigDecimal("-15"));
    }

    @Test
    void maxFutureValueIs200Million() {
        var in = DynamicValueInput.builder()
                .currentMarketValue(new BigDecimal("195000000"))
                .importance(Importance.STARTER)
                .availability(AvailabilityStatus.AVAILABLE)
                .lastMatchdayPoints(20).averagePoints(10.0)
                .minutesPlayed(90).teamReachedFinal(true)
                .build();
        var r = calc.calculateDelta(in);
        assertThat(r.newValue()).isLessThanOrEqualTo(cfg.futureMax);
    }

    @Test
    void minFutureValueIs1Million() {
        var in = DynamicValueInput.builder()
                .currentMarketValue(new BigDecimal("1100000"))
                .importance(Importance.BENCH)
                .availability(AvailabilityStatus.INJURED)
                .lastMatchdayPoints(null).build();
        var r = calc.calculateDelta(in);
        assertThat(r.newValue()).isGreaterThanOrEqualTo(cfg.futureMin);
    }

    @Test
    void resultIncludesReasonAndBreakdown() {
        var in = baseDyn().lastMatchdayPoints(5).averagePoints(3.0).build();
        var r = calc.calculateDelta(in);
        assertThat(r.reason()).isNotNull();
        assertThat(r.breakdown()).containsKeys("momentumScore", "newValue", "reason");
    }
}
