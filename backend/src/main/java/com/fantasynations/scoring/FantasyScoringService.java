package com.fantasynations.scoring;

import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.scoring.dto.PerformanceStats;
import com.fantasynations.scoring.dto.ScoreBreakdown;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure, deterministic per-player scoring engine. Reads every numeric value
 * from {@link ScoringRulesProvider}; never references constants directly.
 *
 * Order of evaluation (per design doc §11.2):
 *   1. DNP -> return 0.
 *   2. Minutes-played base.
 *   3. Goals (position goals vs. penalty goals separated).
 *   4. Assists, big-chance-created.
 *   5. Penalties won / conceded / missed / saved (normal/ET only).
 *   6. Cards, own goals.
 *   7. GK-only: save buckets, GK clean sheet via outfield clean sheet.
 *   8. Clean sheet (>= threshold minutes).
 *   9. Goals conceded (pairs of 2, only onPitchGoalsConceded).
 *  10. Shootout block (independent from normal events).
 *  11. Optional stats (each rule respects {@code enabled}).
 *
 * Tournament-level zeroing (eliminated, not-qualified, matchday-7 slot) is
 * the aggregator's responsibility, NOT this service's.
 */
@Service
public class FantasyScoringService {

    private final ScoringRulesProvider rules;

    public FantasyScoringService(ScoringRulesProvider rules) {
        this.rules = rules;
    }

    public ScoreBreakdown calculate(PerformanceStats s) {
        if (s.didNotPlay()) return ScoreBreakdown.zero();

        Map<String, Integer> b = new LinkedHashMap<>();
        int total = 0;

        // 2. minutes
        int min = s.minutesPlayed();
        if (min >= rules.minutes60PlusThreshold()) total += put(b, "minutes", rules.minutes60Plus());
        else if (min >= 1)                         total += put(b, "minutes", rules.minutesUnder60());

        // 3. goals
        int positionGoals = s.events().goals();
        int penaltyGoals  = s.events().penaltyGoals();
        if (positionGoals > 0) total += put(b, "goals", positionGoals * rules.pointsPerGoal(s.position()));
        if (penaltyGoals > 0)  total += put(b, "penaltyGoals", penaltyGoals * rules.penaltyGoal());

        // 4. assists + big chance created
        if (s.events().assists() > 0)
            total += put(b, "assists", s.events().assists() * rules.assist());
        if (s.events().bigChancesCreated() > 0 && rules.isEnabled("BIG_CHANCE_CREATED"))
            total += put(b, "bigChancesCreated", s.events().bigChancesCreated() * rules.bigChanceCreated());

        // 5. penalty events (normal/ET)
        if (s.events().penaltiesWon() > 0)
            total += put(b, "penaltiesWon", s.events().penaltiesWon() * rules.penaltyWon());
        if (s.events().penaltiesConceded() > 0)
            total += put(b, "penaltiesConceded", s.events().penaltiesConceded() * rules.penaltyConceded());
        if (s.events().penaltiesMissed() > 0)
            total += put(b, "penaltiesMissed", s.events().penaltiesMissed() * rules.penaltyMissed());

        // 6. cards & own goals
        if (s.events().yellowCards() > 0)
            total += put(b, "yellowCards", s.events().yellowCards() * rules.yellowCard());
        if (s.events().doubleYellows() > 0)
            total += put(b, "doubleYellows", s.events().doubleYellows() * rules.doubleYellow());
        if (s.events().directReds() > 0)
            total += put(b, "directReds", s.events().directReds() * rules.directRed());
        if (s.events().ownGoals() > 0)
            total += put(b, "ownGoals", s.events().ownGoals() * rules.ownGoal());

        // 7. GK-only buckets + penalty save
        if (s.position() == PlayerPosition.GK) {
            int saves      = s.events().saves();
            int bucketSize = rules.saveBucketSize();
            int bonusSize  = rules.saveBucketBonusSize();
            if (saves > 0 && bucketSize > 0) {
                int byBucket = (saves / bucketSize) * rules.saveBucketValue();
                if (byBucket != 0) total += put(b, "saveBucket", byBucket);
            }
            if (saves > 0 && bonusSize > 0) {
                int bonus = (saves / bonusSize) * rules.saveBucketBonusValue();
                if (bonus != 0) total += put(b, "saveBucketBonus", bonus);
            }
            if (s.events().penaltiesSavedByGk() > 0)
                total += put(b, "penaltySaved", s.events().penaltiesSavedByGk() * rules.penaltySaved());
        }

        // 8. clean sheet (>= threshold minutes)
        if (s.teamCleanSheet() && min >= rules.cleanSheetThreshold()) {
            int cs = rules.cleanSheet(s.position());
            if (cs != 0) total += put(b, "cleanSheet", cs);
        }

        // 9. goals conceded (pairs of 2, on pitch only)
        int conceded = s.onPitchGoalsConceded();
        int concededBucket = rules.concededBucketSize(s.position());
        if (conceded > 0 && concededBucket > 0) {
            int delta = (conceded / concededBucket) * rules.conceded(s.position());
            if (delta != 0) total += put(b, "goalsConceded", delta);
        }

        // 10. shootout
        if (s.shootout().goals() > 0)
            total += put(b, "shootoutGoals", s.shootout().goals() * rules.shootoutGoal());
        if (s.shootout().misses() > 0)
            total += put(b, "shootoutMisses", s.shootout().misses() * rules.shootoutMiss());
        if (s.shootout().savesByGk() > 0 && s.position() == PlayerPosition.GK)
            total += put(b, "shootoutSaves", s.shootout().savesByGk() * rules.shootoutSave());

        // 11. optional stats (each respects enabled)
        total += applyOptionalBucket(b, "shotsOnTarget",      "SHOTS_ON_TARGET",     s.optional().shotsOnTarget());
        total += applyOptionalBucket(b, "successfulDribbles", "SUCCESSFUL_DRIBBLES", s.optional().successfulDribbles());
        total += applyOptionalBucket(b, "keyPasses",          "KEY_PASSES",          s.optional().keyPasses());
        total += applyOptionalBucket(b, "duelsInterceptions", "DUELS_INTERCEPTIONS", s.optional().duelsWonPlusInterceptions());
        total += applyOptionalBucket(b, "clearances",         "CLEARANCES",          s.optional().clearances());
        total += applyOptionalFlat(b,   "bigChancesMissed",   "BIG_CHANCE_MISSED",   s.optional().bigChancesMissed());
        total += applyOptionalFlat(b,   "errorLeadingToGoal", "ERROR_LEADING_GOAL",  s.optional().errorLeadingToGoal());

        return new ScoreBreakdown(total, b);
    }

    // --- helpers ----------------------------------------------------------

    private int applyOptionalBucket(Map<String, Integer> b, String key, String code, int stat) {
        if (stat <= 0 || !rules.isEnabled(code)) return 0;
        int bucket = rules.bucketSize(code);
        if (bucket <= 0) return 0;
        int delta = (stat / bucket) * rules.value(code);
        if (delta == 0) return 0;
        return put(b, key, delta);
    }

    private int applyOptionalFlat(Map<String, Integer> b, String key, String code, int count) {
        if (count <= 0 || !rules.isEnabled(code)) return 0;
        int delta = count * rules.value(code);
        if (delta == 0) return 0;
        return put(b, key, delta);
    }

    private int put(Map<String, Integer> b, String key, int v) {
        b.merge(key, v, Integer::sum);
        return v;
    }
}
