package com.fantasynations.scoring;

import com.fantasynations.domain.EventScope;
import com.fantasynations.domain.ScoringRuleCategory;
import com.fantasynations.domain.ScoringRulePosition;
import com.fantasynations.entity.ScoringRuleEntity;
import com.fantasynations.repository.ScoringRuleRepository;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Shared test helper: builds a {@link ScoringRulesProvider} pre-seeded with the
 *  default rules from V13. Mirrors what the production app loads from the DB. */
public final class TestScoringRules {

    private TestScoringRules() {}

    public static ScoringRulesProvider provider() {
        ScoringRuleRepository repo = mock(ScoringRuleRepository.class);
        when(repo.findAll()).thenReturn(defaultRules());
        ScoringRulesProvider provider = new ScoringRulesProvider(repo);
        provider.reload();
        return provider;
    }

    private static List<ScoringRuleEntity> defaultRules() {
        var rules = new ArrayList<ScoringRuleEntity>();
        add(rules, "MINUTES_UNDER_60", 1, ScoringRuleCategory.BASE, ScoringRulePosition.ANY, 1,  null, EventScope.ANY, true);
        add(rules, "MINUTES_60_PLUS",  2, ScoringRuleCategory.BASE, ScoringRulePosition.ANY, 60, null, EventScope.ANY, true);

        add(rules, "GOAL_GK",  6, ScoringRuleCategory.BASE, ScoringRulePosition.GK,  null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "GOAL_DEF", 5, ScoringRuleCategory.BASE, ScoringRulePosition.DEF, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "GOAL_MID", 4, ScoringRuleCategory.BASE, ScoringRulePosition.MID, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "GOAL_FWD", 3, ScoringRuleCategory.BASE, ScoringRulePosition.FWD, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);

        add(rules, "PENALTY_GOAL",      3,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "ASSIST",            3,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "PENALTY_WON",       2,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "PENALTY_CONCEDED", -2,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "PENALTY_MISSED",   -2,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "YELLOW_CARD",      -1,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "DOUBLE_YELLOW",    -3,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "DIRECT_RED",       -6,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "OWN_GOAL",         -2,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);

        add(rules, "SAVE_BUCKET",       1, ScoringRuleCategory.GK, ScoringRulePosition.GK, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "SAVE_BUCKET_BONUS", 1, ScoringRuleCategory.GK, ScoringRulePosition.GK, null, 4, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "PENALTY_SAVED",     5, ScoringRuleCategory.GK, ScoringRulePosition.GK, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);

        add(rules, "CLEAN_SHEET_GK",  4, ScoringRuleCategory.CLEAN_SHEET, ScoringRulePosition.GK,  60, null, EventScope.ANY, true);
        add(rules, "CLEAN_SHEET_DEF", 3, ScoringRuleCategory.CLEAN_SHEET, ScoringRulePosition.DEF, 60, null, EventScope.ANY, true);
        add(rules, "CLEAN_SHEET_MID", 2, ScoringRuleCategory.CLEAN_SHEET, ScoringRulePosition.MID, 60, null, EventScope.ANY, true);
        add(rules, "CLEAN_SHEET_FWD", 1, ScoringRuleCategory.CLEAN_SHEET, ScoringRulePosition.FWD, 60, null, EventScope.ANY, true);

        add(rules, "CONCEDED_GK",  -2, ScoringRuleCategory.CONCEDED, ScoringRulePosition.GK,  null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "CONCEDED_DEF", -2, ScoringRuleCategory.CONCEDED, ScoringRulePosition.DEF, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "CONCEDED_MID", -1, ScoringRuleCategory.CONCEDED, ScoringRulePosition.MID, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);
        add(rules, "CONCEDED_FWD", -1, ScoringRuleCategory.CONCEDED, ScoringRulePosition.FWD, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);

        add(rules, "SHOOTOUT_GOAL",  1, ScoringRuleCategory.SHOOTOUT, ScoringRulePosition.ANY, null, null, EventScope.SHOOTOUT, true);
        add(rules, "SHOOTOUT_MISS", -1, ScoringRuleCategory.SHOOTOUT, ScoringRulePosition.ANY, null, null, EventScope.SHOOTOUT, true);
        add(rules, "SHOOTOUT_SAVE",  3, ScoringRuleCategory.SHOOTOUT, ScoringRulePosition.GK,  null, null, EventScope.SHOOTOUT, true);

        // Optional rules disabled (default per V13).
        add(rules, "BIG_CHANCE_CREATED",  1, ScoringRuleCategory.OPTIONAL, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, false);
        add(rules, "SHOTS_ON_TARGET",     1, ScoringRuleCategory.OPTIONAL, ScoringRulePosition.ANY, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, false);

        return rules;
    }

    private static void add(List<ScoringRuleEntity> list, String code, int value, ScoringRuleCategory cat,
                            ScoringRulePosition pos, Integer threshold, Integer bucket, EventScope scope, boolean enabled) {
        list.add(ScoringRuleEntity.builder()
                .code(code).value(value).category(cat).position(pos)
                .threshold(threshold).bucketSize(bucket).eventScope(scope).enabled(enabled).build());
    }
}
