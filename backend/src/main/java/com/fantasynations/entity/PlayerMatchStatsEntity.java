package com.fantasynations.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "player_match_stats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "real_match_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerMatchStatsEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "real_match_id", nullable = false)
    private UUID realMatchId;

    @Column(name = "did_not_play", nullable = false)
    private boolean didNotPlay;

    @Column(name = "minutes_played", nullable = false)
    private int minutesPlayed;

    @Column(name = "on_pitch_goals_conceded", nullable = false)
    private int onPitchGoalsConceded;

    @Column(name = "team_clean_sheet", nullable = false)
    private boolean teamCleanSheet;

    // normal/extra-time events
    @Column(name = "goals",                  nullable = false) private int goals;
    @Column(name = "penalty_goals",          nullable = false) private int penaltyGoals;
    @Column(name = "assists",                nullable = false) private int assists;
    @Column(name = "big_chances_created",    nullable = false) private int bigChancesCreated;
    @Column(name = "penalties_won",          nullable = false) private int penaltiesWon;
    @Column(name = "penalties_conceded",     nullable = false) private int penaltiesConceded;
    @Column(name = "penalties_missed",       nullable = false) private int penaltiesMissed;
    @Column(name = "penalties_saved_by_gk",  nullable = false) private int penaltiesSavedByGk;
    @Column(name = "saves",                  nullable = false) private int saves;
    @Column(name = "yellow_cards",           nullable = false) private int yellowCards;
    @Column(name = "double_yellows",         nullable = false) private int doubleYellows;
    @Column(name = "direct_reds",            nullable = false) private int directReds;
    @Column(name = "own_goals",              nullable = false) private int ownGoals;

    // shootout events
    @Column(name = "shootout_goals",         nullable = false) private int shootoutGoals;
    @Column(name = "shootout_misses",        nullable = false) private int shootoutMisses;
    @Column(name = "shootout_saves_by_gk",   nullable = false) private int shootoutSavesByGk;

    // optional stats
    @Column(name = "shots_on_target",                nullable = false) private int shotsOnTarget;
    @Column(name = "successful_dribbles",            nullable = false) private int successfulDribbles;
    @Column(name = "key_passes",                     nullable = false) private int keyPasses;
    @Column(name = "duels_won_plus_interceptions",   nullable = false) private int duelsWonPlusInterceptions;
    @Column(name = "clearances",                     nullable = false) private int clearances;
    @Column(name = "big_chances_missed",             nullable = false) private int bigChancesMissed;
    @Column(name = "error_leading_to_goal",          nullable = false) private int errorLeadingToGoal;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
