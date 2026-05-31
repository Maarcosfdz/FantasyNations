package com.fantasynations.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Admin payload for posting stats for one player in one real match.
 * Every numeric field defaults to 0; only set what changed.
 */
public record PlayerMatchStatsRequestDto(
        @NotNull UUID playerId,
        boolean didNotPlay,
        int minutesPlayed,
        int onPitchGoalsConceded,
        boolean teamCleanSheet,
        // normal/ET
        int goals,
        int penaltyGoals,
        int assists,
        int bigChancesCreated,
        int penaltiesWon,
        int penaltiesConceded,
        int penaltiesMissed,
        int penaltiesSavedByGk,
        int saves,
        int yellowCards,
        int doubleYellows,
        int directReds,
        int ownGoals,
        // shootout
        int shootoutGoals,
        int shootoutMisses,
        int shootoutSavesByGk,
        // optional
        int shotsOnTarget,
        int successfulDribbles,
        int keyPasses,
        int duelsWonPlusInterceptions,
        int clearances,
        int bigChancesMissed,
        int errorLeadingToGoal
) {}
