package com.fantasynations.marketvalue;

import com.fantasynations.domain.AvailabilityStatus;
import com.fantasynations.domain.Importance;
import com.fantasynations.domain.LeagueReputation;
import com.fantasynations.domain.NationalTeamTier;
import com.fantasynations.domain.PlayerPosition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for every numeric constant used by the market-value
 * algorithm. Services and the calculator must reference values from here -
 * no magic numbers anywhere else.
 */
@Component
public class MarketValueConfig {

    // --- money / global ------------------------------------------------------

    public final BigDecimal startingUserBudget = bd(300_000_000);

    public final BigDecimal initialMin = bd(1_000_000);
    public final BigDecimal initialMax = bd(70_000_000);
    public final BigDecimal futureMin  = bd(1_000_000);
    public final BigDecimal futureMax  = bd(300_000_000);

    /** Rounding step for stored values (BigDecimal.scale = 0). */
    public final BigDecimal roundingUnit = bd(100_000);

    /** Hard caps for a single update cycle. Percent points, not basis points. */
    public final BigDecimal maxIncreasePercentPerCycle = bd(15);
    public final BigDecimal maxDecreasePercentPerCycle = bd(-15);

    // --- initial value: position base ----------------------------------------

    public final Map<PlayerPosition, BigDecimal> positionBaseValue = Map.of(
            PlayerPosition.GK,  bd( 4_000_000),
            PlayerPosition.DEF, bd( 6_000_000),
            PlayerPosition.MID, bd( 8_000_000),
            PlayerPosition.FWD, bd(10_000_000)
    );

    // --- initial value: tier bonus -------------------------------------------

    public final Map<NationalTeamTier, BigDecimal> tierBonus = Map.of(
            NationalTeamTier.S, bd(10_000_000),
            NationalTeamTier.A, bd( 6_000_000),
            NationalTeamTier.B, bd( 3_000_000),
            NationalTeamTier.C, bd(         0)
    );

    public final Set<String> tierS = Set.of(
            "Argentina", "Brazil", "France", "England",
            "Spain", "Portugal", "Germany", "Netherlands"
    );
    public final Set<String> tierA = Set.of(
            "Belgium", "Croatia", "Uruguay", "Switzerland",
            "Denmark", "Senegal", "Morocco", "United States",
            "Mexico", "Japan"
    );
    public final Set<String> tierB = Set.of(
            "Serbia", "Poland", "Ghana", "South Korea",
            "Ecuador", "Wales", "Cameroon", "Canada", "Australia"
    );
    public final Set<String> tierC = Set.of(
            "Qatar", "Saudi Arabia", "Iran", "Tunisia", "Costa Rica"
    );

    // --- initial value: importance / reputation ------------------------------

    public final Map<Importance, BigDecimal> importanceBonus = Map.of(
            Importance.GLOBAL_SUPERSTAR, bd(40_000_000),
            Importance.STAR,             bd(30_000_000),
            Importance.STARTER,          bd(12_000_000),
            Importance.ROTATION,         bd( 4_000_000),
            Importance.BENCH,            bd(         0)
    );

    public final Map<LeagueReputation, BigDecimal> leagueReputationBonus = Map.of(
            LeagueReputation.ELITE,  bd(10_000_000),
            LeagueReputation.STRONG, bd( 6_000_000),
            LeagueReputation.MEDIUM, bd( 3_000_000),
            LeagueReputation.LOW,    bd(         0)
    );

    /** Used when the importer falls back to assigning importance by lineup order. */
    public final int fallbackStartersGK  = 1;
    public final int fallbackStartersDEF = 4;
    public final int fallbackStartersMID = 4;
    public final int fallbackStartersFWD = 3;

    // --- dynamic value: momentum -> percent delta ----------------------------

    /**
     * Ordered ascending by lower-bound score. Each band has [min, percent].
     * Walk descending to find the first band whose `min` is <= score.
     * The thresholds reflect the spec directly.
     */
    public final int[][] momentumToPercent = new int[][] {
            { -7,  -12 }, // score < -6
            { -6,   -7 }, // -6 <= score <= -4
            { -3,   -3 }, // -3 <= score <= -1   (spec: score >= -3 -> -3%)
            {  0,    0 },
            {  1,    2 },
            {  4,    4 },
            {  7,    7 },
            { 10,   10 },
            { 14,   15 }
    };

    // --- dynamic value: subscores --------------------------------------------

    /** [pointsAtLeast, score]. Highest matching wins. */
    public final int[][] pointsScoreTable = new int[][] {
            { 15,  8 },
            { 10,  6 },
            {  7,  4 },
            {  4,  2 },
            {  1,  1 }
    };
    public final int pointsScoreZero     = -1;
    public final int pointsScoreNegative = -4;

    public final int[][] avgPointsModifierTable = new int[][] {
            { 8,  3 },
            { 6,  2 },
            { 4,  1 },
            { 2,  0 }
    };
    public final int avgPointsBelowTwoModifier = -1;

    public final int minutesScore90Plus    =  2;
    public final int minutesScore60To89    =  1;
    public final int minutesScore1To59     =  0;
    public final int minutesScoreDidNotPlay = -3;

    public final Map<AvailabilityStatus, Integer> availabilityScore = Map.of(
            AvailabilityStatus.AVAILABLE,         0,
            AvailabilityStatus.DOUBTFUL,         -2,
            AvailabilityStatus.INJURED,          -6,
            AvailabilityStatus.SUSPENDED,        -4,
            AvailabilityStatus.OUT_OF_TOURNAMENT, -8
    );

    public final int tournamentStillAlive   =  0;
    public final int tournamentEliminated   = -6;
    public final int tournamentSemiFinals   =  1;
    public final int tournamentFinal        =  2;

    // --- dynamic value: special-rule minimum penalties ----------------------

    public final BigDecimal injuredMinPenaltyPercent     = bd(-8);
    public final BigDecimal suspendedMinPenaltyPercent   = bd(-5);
    public final BigDecimal eliminatedMinPenaltyPercent  = bd(-10);
    public final BigDecimal didNotPlayMinPenaltyPercent  = bd(-5);

    // --- market mechanics (used by slice B; values fixed now) ----------------

    public final BigDecimal quickSellPercent       = bd(50);
    public final BigDecimal machineOfferRangePct   = bd(10);

    // --- release clauses ------------------------------------------------------

    public final Map<Importance, BigDecimal> autoClauseMultiplier = Map.of(
            Importance.BENCH,             bd("1.10"),
            Importance.ROTATION,          bd("1.15"),
            Importance.STARTER,           bd("1.20"),
            Importance.STAR,              bd("1.30"),
            Importance.GLOBAL_SUPERSTAR,  bd("1.40")
    );
    public final BigDecimal defaultAutoClauseMultiplier = bd("1.20"); // when importance missing

    // --- helpers --------------------------------------------------------------

    private static BigDecimal bd(long v) { return BigDecimal.valueOf(v); }
    private static BigDecimal bd(String s) { return new BigDecimal(s); }
}
