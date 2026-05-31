package com.fantasynations.scoring;

import com.fantasynations.domain.EventScope;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.domain.ScoringRuleCategory;
import com.fantasynations.domain.ScoringRulePosition;
import com.fantasynations.entity.ScoringRuleEntity;
import com.fantasynations.repository.ScoringRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScoringRulesProviderTest {

    private ScoringRuleRepository repository;
    private ScoringRulesProvider provider;
    private final List<ScoringRuleEntity> rules = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repository = mock(ScoringRuleRepository.class);
        rules.clear();
        seedDefaults();
        when(repository.findAll()).thenReturn(rules);
        provider = new ScoringRulesProvider(repository);
        provider.reload();
    }

    @Test
    void loadsDefaultRulesFromRepository() {
        assertThat(provider.minutesUnder60()).isEqualTo(1);
        assertThat(provider.minutes60Plus()).isEqualTo(2);
        assertThat(provider.minutes60PlusThreshold()).isEqualTo(60);
    }

    @Test
    void pointsPerGoalDifferByPosition() {
        assertThat(provider.pointsPerGoal(PlayerPosition.GK)).isEqualTo(6);
        assertThat(provider.pointsPerGoal(PlayerPosition.DEF)).isEqualTo(5);
        assertThat(provider.pointsPerGoal(PlayerPosition.MID)).isEqualTo(4);
        assertThat(provider.pointsPerGoal(PlayerPosition.FWD)).isEqualTo(3);
    }

    @Test
    void cleanSheetPointsRequire60MinThreshold() {
        assertThat(provider.cleanSheetThreshold()).isEqualTo(60);
        assertThat(provider.cleanSheet(PlayerPosition.GK)).isEqualTo(4);
        assertThat(provider.cleanSheet(PlayerPosition.DEF)).isEqualTo(3);
        assertThat(provider.cleanSheet(PlayerPosition.MID)).isEqualTo(2);
        assertThat(provider.cleanSheet(PlayerPosition.FWD)).isEqualTo(1);
    }

    @Test
    void concededBucketSizeIsTwoForAllPositions() {
        for (PlayerPosition p : PlayerPosition.values()) {
            assertThat(provider.concededBucketSize(p)).isEqualTo(2);
        }
        assertThat(provider.conceded(PlayerPosition.GK)).isEqualTo(-2);
        assertThat(provider.conceded(PlayerPosition.DEF)).isEqualTo(-2);
        assertThat(provider.conceded(PlayerPosition.MID)).isEqualTo(-1);
        assertThat(provider.conceded(PlayerPosition.FWD)).isEqualTo(-1);
    }

    @Test
    void saveBucketsExposeValueAndBucketSize() {
        assertThat(provider.saveBucketValue()).isEqualTo(1);
        assertThat(provider.saveBucketSize()).isEqualTo(2);
        assertThat(provider.saveBucketBonusValue()).isEqualTo(1);
        assertThat(provider.saveBucketBonusSize()).isEqualTo(4);
    }

    @Test
    void shootoutValuesAreSeparateFromNormalEvents() {
        assertThat(provider.shootoutGoal()).isEqualTo(1);
        assertThat(provider.shootoutMiss()).isEqualTo(-1);
        assertThat(provider.shootoutSave()).isEqualTo(3);
        assertThat(provider.penaltySaved()).isEqualTo(5);
        assertThat(provider.eventScopeOf("SHOOTOUT_GOAL")).contains(EventScope.SHOOTOUT);
        assertThat(provider.eventScopeOf("PENALTY_SAVED")).contains(EventScope.NORMAL_OR_EXTRA_TIME);
    }

    @Test
    void disabledRuleReturnsZeroValueButRuleIsStillResolvable() {
        addRule("SHOTS_ON_TARGET", 1, ScoringRuleCategory.OPTIONAL,
                ScoringRulePosition.ANY, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, false);
        provider.reload();

        assertThat(provider.isEnabled("SHOTS_ON_TARGET")).isFalse();
        assertThat(provider.value("SHOTS_ON_TARGET")).isZero();
        assertThat(provider.categoryOf("SHOTS_ON_TARGET")).contains(ScoringRuleCategory.OPTIONAL);
    }

    @Test
    void reloadPicksUpUpdatedValuesWithoutRestart() {
        assertThat(provider.pointsPerGoal(PlayerPosition.FWD)).isEqualTo(3);

        findRule("GOAL_FWD").setValue(10);
        provider.reload();

        assertThat(provider.pointsPerGoal(PlayerPosition.FWD)).isEqualTo(10);
    }

    @Test
    void missingThresholdOrBucketThrowsClearError() {
        assertThatThrownBy(() -> provider.threshold("GOAL_FWD"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("threshold");
        assertThatThrownBy(() -> provider.bucketSize("GOAL_FWD"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bucket_size");
    }

    @Test
    void unknownRuleHasZeroValueAndIsNotEnabled() {
        assertThat(provider.value("DOES_NOT_EXIST")).isZero();
        assertThat(provider.isEnabled("DOES_NOT_EXIST")).isFalse();
        assertThat(provider.rule("DOES_NOT_EXIST")).isEmpty();
    }

    // --- helpers ---

    private void seedDefaults() {
        addRule("MINUTES_UNDER_60", 1, ScoringRuleCategory.BASE, ScoringRulePosition.ANY, 1,  null, EventScope.ANY, true);
        addRule("MINUTES_60_PLUS",  2, ScoringRuleCategory.BASE, ScoringRulePosition.ANY, 60, null, EventScope.ANY, true);

        addRule("GOAL_GK",  6, ScoringRuleCategory.BASE, ScoringRulePosition.GK,  null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("GOAL_DEF", 5, ScoringRuleCategory.BASE, ScoringRulePosition.DEF, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("GOAL_MID", 4, ScoringRuleCategory.BASE, ScoringRulePosition.MID, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("GOAL_FWD", 3, ScoringRuleCategory.BASE, ScoringRulePosition.FWD, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);

        addRule("PENALTY_GOAL",     3,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("ASSIST",           3,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("PENALTY_WON",      2,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("PENALTY_CONCEDED",-2,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("PENALTY_MISSED",  -2,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("YELLOW_CARD",     -1,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("DOUBLE_YELLOW",   -3,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("DIRECT_RED",      -6,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("OWN_GOAL",        -2,  ScoringRuleCategory.BASE, ScoringRulePosition.ANY, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);

        addRule("SAVE_BUCKET",       1, ScoringRuleCategory.GK, ScoringRulePosition.GK, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("SAVE_BUCKET_BONUS", 1, ScoringRuleCategory.GK, ScoringRulePosition.GK, null, 4, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("PENALTY_SAVED",     5, ScoringRuleCategory.GK, ScoringRulePosition.GK, null, null, EventScope.NORMAL_OR_EXTRA_TIME, true);

        addRule("CLEAN_SHEET_GK",  4, ScoringRuleCategory.CLEAN_SHEET, ScoringRulePosition.GK,  60, null, EventScope.ANY, true);
        addRule("CLEAN_SHEET_DEF", 3, ScoringRuleCategory.CLEAN_SHEET, ScoringRulePosition.DEF, 60, null, EventScope.ANY, true);
        addRule("CLEAN_SHEET_MID", 2, ScoringRuleCategory.CLEAN_SHEET, ScoringRulePosition.MID, 60, null, EventScope.ANY, true);
        addRule("CLEAN_SHEET_FWD", 1, ScoringRuleCategory.CLEAN_SHEET, ScoringRulePosition.FWD, 60, null, EventScope.ANY, true);

        addRule("CONCEDED_GK",  -2, ScoringRuleCategory.CONCEDED, ScoringRulePosition.GK,  null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("CONCEDED_DEF", -2, ScoringRuleCategory.CONCEDED, ScoringRulePosition.DEF, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("CONCEDED_MID", -1, ScoringRuleCategory.CONCEDED, ScoringRulePosition.MID, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);
        addRule("CONCEDED_FWD", -1, ScoringRuleCategory.CONCEDED, ScoringRulePosition.FWD, null, 2, EventScope.NORMAL_OR_EXTRA_TIME, true);

        addRule("SHOOTOUT_GOAL",  1, ScoringRuleCategory.SHOOTOUT, ScoringRulePosition.ANY, null, null, EventScope.SHOOTOUT, true);
        addRule("SHOOTOUT_MISS", -1, ScoringRuleCategory.SHOOTOUT, ScoringRulePosition.ANY, null, null, EventScope.SHOOTOUT, true);
        addRule("SHOOTOUT_SAVE",  3, ScoringRuleCategory.SHOOTOUT, ScoringRulePosition.GK,  null, null, EventScope.SHOOTOUT, true);
    }

    private void addRule(String code, int value, ScoringRuleCategory cat, ScoringRulePosition pos,
                         Integer threshold, Integer bucketSize, EventScope scope, boolean enabled) {
        rules.add(ScoringRuleEntity.builder()
                .code(code)
                .value(value)
                .category(cat)
                .position(pos)
                .threshold(threshold)
                .bucketSize(bucketSize)
                .eventScope(scope)
                .enabled(enabled)
                .build());
    }

    private ScoringRuleEntity findRule(String code) {
        return rules.stream().filter(r -> r.getCode().equals(code)).findFirst().orElseThrow();
    }
}
