package com.fantasynations.scoring.dto;

/** Normal-time + extra-time events (NOT shootout). */
public record MatchEvents(
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
        int ownGoals
) {
    public static MatchEvents empty() {
        return new MatchEvents(0,0,0,0,0,0,0,0,0,0,0,0,0);
    }
}
