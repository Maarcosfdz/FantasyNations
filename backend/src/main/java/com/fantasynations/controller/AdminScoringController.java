package com.fantasynations.controller;

import com.fantasynations.dto.PlayerMatchStatsRequestDto;
import com.fantasynations.marketvalue.DynamicMarketValueService;
import com.fantasynations.scoring.MatchdayAggregationService;
import com.fantasynations.scoring.PlayerMatchStatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin / internal endpoints that drive the scoring engine end-to-end.
 * Not protected at controller level - rely on existing security config to
 * lock them down to admins in production.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class AdminScoringController {

    private final PlayerMatchStatsService statsService;
    private final MatchdayAggregationService aggregationService;
    private final DynamicMarketValueService dynamicMarketValueService;

    /** Upsert a single player's stats for a real match. */
    @PostMapping("/matches/{realMatchId}/stats")
    public ResponseEntity<Void> postStat(
            @PathVariable UUID realMatchId,
            @Valid @RequestBody PlayerMatchStatsRequestDto dto) {
        statsService.upsert(realMatchId, dto);
        return ResponseEntity.noContent().build();
    }

    /** Bulk variant: post all 22 players for a match in one call. */
    @PostMapping("/matches/{realMatchId}/stats/bulk")
    public ResponseEntity<Void> postStatsBulk(
            @PathVariable UUID realMatchId,
            @RequestBody List<PlayerMatchStatsRequestDto> dtos) {
        for (PlayerMatchStatsRequestDto dto : dtos) {
            statsService.upsert(realMatchId, dto);
        }
        return ResponseEntity.noContent().build();
    }

    /** Trigger aggregation for one user / one matchday. */
    @PostMapping("/leagues/{leagueId}/users/{userId}/matchdays/{matchdayId}/aggregate")
    public ResponseEntity<MatchdayAggregationService.AggregationResult> aggregateOne(
            @PathVariable UUID leagueId,
            @PathVariable UUID userId,
            @PathVariable UUID matchdayId) {
        return ResponseEntity.ok(aggregationService.aggregate(leagueId, userId, matchdayId));
    }

    /** Trigger aggregation for every member of a league for a matchday. */
    @PostMapping("/leagues/{leagueId}/matchdays/{matchdayId}/aggregate")
    public ResponseEntity<List<MatchdayAggregationService.AggregationResult>> aggregateLeague(
            @PathVariable UUID leagueId,
            @PathVariable UUID matchdayId) {
        return ResponseEntity.ok(aggregationService.aggregateAllMembers(leagueId, matchdayId));
    }

    /** Apply the dynamic market-value update for a given matchday. Idempotent. */
    @PostMapping("/matchdays/{matchdayId}/apply-market-value")
    public ResponseEntity<DynamicMarketValueService.ApplyResult> applyMarketValue(
            @PathVariable UUID matchdayId) {
        return ResponseEntity.ok(dynamicMarketValueService.applyForMatchday(matchdayId));
    }
}
