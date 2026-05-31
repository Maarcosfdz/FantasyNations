package com.fantasynations.scoring;

import com.fantasynations.domain.EventScope;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.domain.ScoringRuleCategory;
import com.fantasynations.domain.ScoringRulePosition;
import com.fantasynations.entity.ScoringRuleEntity;
import com.fantasynations.repository.ScoringRuleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads scoring rules from the database once at startup and exposes typed
 * accessors. No numeric literals for points, thresholds, or bucket sizes
 * should ever live in the scoring service - all values come from here.
 */
@Component
public class ScoringRulesProvider {

    private final ScoringRuleRepository repository;
    private final Map<String, ScoringRuleEntity> byCode = new HashMap<>();

    public ScoringRulesProvider(ScoringRuleRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public synchronized void reload() {
        byCode.clear();
        List<ScoringRuleEntity> all = repository.findAll();
        for (ScoringRuleEntity rule : all) {
            byCode.put(rule.getCode(), rule);
        }
    }

    public Optional<ScoringRuleEntity> rule(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    public boolean isEnabled(String code) {
        ScoringRuleEntity rule = byCode.get(code);
        return rule != null && rule.isEnabled();
    }

    public int value(String code) {
        ScoringRuleEntity rule = byCode.get(code);
        if (rule == null || !rule.isEnabled()) return 0;
        return rule.getValue();
    }

    public int threshold(String code) {
        ScoringRuleEntity rule = byCode.get(code);
        if (rule == null || rule.getThreshold() == null) {
            throw new IllegalStateException("Rule " + code + " has no threshold configured");
        }
        return rule.getThreshold();
    }

    public int bucketSize(String code) {
        ScoringRuleEntity rule = byCode.get(code);
        if (rule == null || rule.getBucketSize() == null) {
            throw new IllegalStateException("Rule " + code + " has no bucket_size configured");
        }
        return rule.getBucketSize();
    }

    // -- Typed accessors covering the spec --

    public int minutesUnder60() { return value("MINUTES_UNDER_60"); }
    public int minutes60Plus()  { return value("MINUTES_60_PLUS"); }
    public int minutes60PlusThreshold() { return threshold("MINUTES_60_PLUS"); }

    public int pointsPerGoal(PlayerPosition position) {
        return value("GOAL_" + position.name());
    }

    public int cleanSheet(PlayerPosition position) {
        return value("CLEAN_SHEET_" + position.name());
    }

    public int cleanSheetThreshold() { return threshold("CLEAN_SHEET_GK"); }

    public int conceded(PlayerPosition position) {
        return value("CONCEDED_" + position.name());
    }

    public int concededBucketSize(PlayerPosition position) {
        return bucketSize("CONCEDED_" + position.name());
    }

    public int saveBucketValue() { return value("SAVE_BUCKET"); }
    public int saveBucketSize()  { return bucketSize("SAVE_BUCKET"); }
    public int saveBucketBonusValue() { return value("SAVE_BUCKET_BONUS"); }
    public int saveBucketBonusSize()  { return bucketSize("SAVE_BUCKET_BONUS"); }

    public int penaltyGoal()      { return value("PENALTY_GOAL"); }
    public int assist()           { return value("ASSIST"); }
    public int bigChanceCreated() { return value("BIG_CHANCE_CREATED"); }
    public int penaltyWon()       { return value("PENALTY_WON"); }
    public int penaltyConceded()  { return value("PENALTY_CONCEDED"); }
    public int penaltyMissed()    { return value("PENALTY_MISSED"); }
    public int penaltySaved()     { return value("PENALTY_SAVED"); }
    public int yellowCard()       { return value("YELLOW_CARD"); }
    public int doubleYellow()     { return value("DOUBLE_YELLOW"); }
    public int directRed()        { return value("DIRECT_RED"); }
    public int ownGoal()          { return value("OWN_GOAL"); }

    public int shootoutGoal() { return value("SHOOTOUT_GOAL"); }
    public int shootoutMiss() { return value("SHOOTOUT_MISS"); }
    public int shootoutSave() { return value("SHOOTOUT_SAVE"); }

    /** Returns the registered category for a rule, useful for tests and admin tooling. */
    public Optional<ScoringRuleCategory> categoryOf(String code) {
        return rule(code).map(ScoringRuleEntity::getCategory);
    }

    public Optional<EventScope> eventScopeOf(String code) {
        return rule(code).map(ScoringRuleEntity::getEventScope);
    }

    public Optional<ScoringRulePosition> positionOf(String code) {
        return rule(code).map(ScoringRuleEntity::getPosition);
    }
}
