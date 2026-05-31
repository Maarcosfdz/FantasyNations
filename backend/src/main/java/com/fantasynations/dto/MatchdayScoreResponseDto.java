package com.fantasynations.dto;

import com.fantasynations.domain.MatchdayAggregationReason;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MatchdayScoreResponseDto(
        UUID matchdayId,
        int matchdayNumber,
        UUID leagueId,
        UUID userId,
        int totalPoints,
        MatchdayAggregationReason reason,
        LocalDateTime aggregatedAt,
        List<PlayerScoreDto> players
) {
    public record PlayerScoreDto(
            UUID playerId,
            String playerName,
            String nationalTeam,
            String position,
            String positionSlot,
            String imageRef,
            int points,
            /** Per-category breakdown of the points. May be empty. */
            java.util.Map<String, Integer> breakdown
    ) {}
}
