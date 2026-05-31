package com.fantasynations.scoring;

import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.scoring.dto.MatchEvents;
import com.fantasynations.scoring.dto.OptionalStats;
import com.fantasynations.scoring.dto.PerformanceStats;
import com.fantasynations.scoring.dto.ScoreBreakdown;
import com.fantasynations.scoring.dto.ShootoutEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FantasyScoringServiceTest {

    private FantasyScoringService service;

    @BeforeEach
    void setUp() {
        service = new FantasyScoringService(TestScoringRules.provider());
    }

    @Test
    void didNotPlayReturnsZero() {
        var perf = base(PlayerPosition.MID).didNotPlay(true).build();
        assertThat(service.calculate(perf).total()).isZero();
    }

    @Test
    void played60PlusBaseIsTwoPoints() {
        var perf = base(PlayerPosition.MID).minutesPlayed(60).build();
        ScoreBreakdown sb = service.calculate(perf);
        assertThat(sb.byCategory()).containsEntry("minutes", 2);
        assertThat(sb.total()).isEqualTo(2);
    }

    @Test
    void playedUnder60BaseIsOnePoint() {
        var perf = base(PlayerPosition.MID).minutesPlayed(30).build();
        assertThat(service.calculate(perf).total()).isEqualTo(1);
    }

    @Test
    void goalByDefenderUsesPositionMultiplier() {
        var perf = base(PlayerPosition.DEF).minutesPlayed(90)
                .events(eventsWith(b -> b.goals = 1)).build();
        // 2 minutes + 5 goal = 7
        assertThat(service.calculate(perf).total()).isEqualTo(7);
    }

    @Test
    void penaltyGoalUsesPenaltyValueNotPositionMultiplier() {
        var perf = base(PlayerPosition.FWD).minutesPlayed(90)
                .events(eventsWith(b -> b.penaltyGoals = 1)).build();
        // 2 minutes + 3 penalty goal = 5  (NOT 2 + 3 position goal = same here but key is the field used)
        ScoreBreakdown sb = service.calculate(perf);
        assertThat(sb.byCategory()).containsEntry("penaltyGoals", 3);
        assertThat(sb.byCategory()).doesNotContainKey("goals");
    }

    @Test
    void gkSaveBucketScoresAcrossBands() {
        // saves: 2 -> +1, 4 -> +3, 6 -> +4, 8 -> +6
        int[][] cases = {{2,1},{4,3},{6,4},{8,6}};
        for (int[] c : cases) {
            var perf = base(PlayerPosition.GK).minutesPlayed(90)
                    .events(eventsWith(b -> b.saves = c[0])).build();
            // exclude minutes (+2) from comparison
            int withoutMin = service.calculate(perf).total() - 2;
            assertThat(withoutMin)
                    .as("saves=%s", c[0])
                    .isEqualTo(c[1]);
        }
    }

    @Test
    void gkPenaltySavedInMatchScoresFive() {
        var perf = base(PlayerPosition.GK).minutesPlayed(90)
                .events(eventsWith(b -> b.penaltiesSavedByGk = 1)).build();
        // 2 minutes + 5 = 7
        assertThat(service.calculate(perf).total()).isEqualTo(7);
    }

    @Test
    void shootoutGoalAndMiss() {
        var goal = base(PlayerPosition.FWD).minutesPlayed(90)
                .shootout(new ShootoutEvents(1, 0, 0)).build();
        var miss = base(PlayerPosition.FWD).minutesPlayed(90)
                .shootout(new ShootoutEvents(0, 1, 0)).build();
        assertThat(service.calculate(goal).byCategory()).containsEntry("shootoutGoals", 1);
        assertThat(service.calculate(miss).byCategory()).containsEntry("shootoutMisses", -1);
    }

    @Test
    void shootoutSaveByGkScoresThreeAndIsSeparateFromMatchPenaltySave() {
        var perf = base(PlayerPosition.GK).minutesPlayed(90)
                .shootout(new ShootoutEvents(0, 0, 1)).build();
        ScoreBreakdown sb = service.calculate(perf);
        assertThat(sb.byCategory()).containsEntry("shootoutSaves", 3);
        assertThat(sb.byCategory()).doesNotContainKey("penaltySaved");
    }

    @Test
    void cleanSheetByPositionWithSixtyMinutes() {
        for (var pair : new Object[][]{
                {PlayerPosition.GK,  4},
                {PlayerPosition.DEF, 3},
                {PlayerPosition.MID, 2},
                {PlayerPosition.FWD, 1}}) {
            PlayerPosition pos = (PlayerPosition) pair[0];
            int expected = (int) pair[1];
            var perf = base(pos).minutesPlayed(90).teamCleanSheet(true).build();
            // 2 minutes + expected = 2 + expected
            assertThat(service.calculate(perf).total())
                    .as("pos=%s", pos)
                    .isEqualTo(2 + expected);
        }
    }

    @Test
    void cleanSheetIgnoredUnderSixtyMinutes() {
        var perf = base(PlayerPosition.DEF).minutesPlayed(45).teamCleanSheet(true).build();
        // 1 minute, no clean sheet => 1
        assertThat(service.calculate(perf).total()).isEqualTo(1);
    }

    @Test
    void goalsConcededPairsOfTwoForDefenders() {
        // 1 conceded: 0, 2 conceded: -2, 3 conceded: -2, 4 conceded: -4
        int[][] cases = {{1,0},{2,-2},{3,-2},{4,-4}};
        for (int[] c : cases) {
            var perf = base(PlayerPosition.DEF).minutesPlayed(90)
                    .onPitchGoalsConceded(c[0]).build();
            // exclude minutes (+2)
            assertThat(service.calculate(perf).total() - 2)
                    .as("conceded=%s", c[0])
                    .isEqualTo(c[1]);
        }
    }

    @Test
    void yellowAndDirectRedDeductPoints() {
        var perf = base(PlayerPosition.MID).minutesPlayed(70)
                .events(eventsWith(b -> { b.yellowCards = 1; b.directReds = 1; })).build();
        // 2 + (-1) + (-6) = -5
        assertThat(service.calculate(perf).total()).isEqualTo(-5);
    }

    @Test
    void ownGoalDeducts() {
        var perf = base(PlayerPosition.DEF).minutesPlayed(90)
                .events(eventsWith(b -> b.ownGoals = 1)).build();
        // 2 minutes + (-2) = 0
        assertThat(service.calculate(perf).total()).isZero();
    }

    @Test
    void disabledOptionalRulesContributeZero() {
        var perf = base(PlayerPosition.MID).minutesPlayed(90)
                .optional(new OptionalStats(10, 10, 10, 20, 10, 5, 5))
                .build();
        // Only minutes count (2). Big-chance-created etc. are disabled.
        assertThat(service.calculate(perf).total()).isEqualTo(2);
    }

    @Test
    void extraTimeEventsAreTreatedLikeNormalTime() {
        // Spec wording: extra-time goals/cards use the same values. The mapper
        // does not distinguish them, so the test simply asserts that values
        // are applied as if they were normal-time events.
        var perf = base(PlayerPosition.FWD).minutesPlayed(120)
                .events(eventsWith(b -> { b.goals = 1; b.yellowCards = 1; })).build();
        // 2 + 3 + (-1) = 4
        assertThat(service.calculate(perf).total()).isEqualTo(4);
    }

    // ---- helpers ----

    private PerformanceStats.Builder base(PlayerPosition pos) {
        return PerformanceStats.builder().position(pos);
    }

    @FunctionalInterface
    private interface EventsCustomizer { void customize(EventsBuilder b); }

    private MatchEvents eventsWith(EventsCustomizer fn) {
        EventsBuilder b = new EventsBuilder();
        fn.customize(b);
        return b.build();
    }

    private static final class EventsBuilder {
        int goals, penaltyGoals, assists, bigChancesCreated, penaltiesWon, penaltiesConceded,
            penaltiesMissed, penaltiesSavedByGk, saves, yellowCards, doubleYellows,
            directReds, ownGoals;
        MatchEvents build() {
            return new MatchEvents(goals, penaltyGoals, assists, bigChancesCreated,
                    penaltiesWon, penaltiesConceded, penaltiesMissed, penaltiesSavedByGk,
                    saves, yellowCards, doubleYellows, directReds, ownGoals);
        }
    }
}
