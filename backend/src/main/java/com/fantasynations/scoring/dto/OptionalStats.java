package com.fantasynations.scoring.dto;

public record OptionalStats(
        int shotsOnTarget,
        int successfulDribbles,
        int keyPasses,
        int duelsWonPlusInterceptions,
        int clearances,
        int bigChancesMissed,
        int errorLeadingToGoal
) {
    public static OptionalStats empty() {
        return new OptionalStats(0, 0, 0, 0, 0, 0, 0);
    }
}
