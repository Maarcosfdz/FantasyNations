package com.fantasynations.marketvalue;

import com.fantasynations.entity.MatchdayEntity;
import com.fantasynations.entity.MarketValueHistoryEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.PlayerMatchStatsEntity;
import com.fantasynations.entity.RealMatchEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.MarketValueHistoryRepository;
import com.fantasynations.repository.MatchdayRepository;
import com.fantasynations.repository.PlayerMatchStatsRepository;
import com.fantasynations.repository.PlayerMatchdayScoreRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fantasynations.repository.RealMatchRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Slice C of the market-value system: after a matchday is aggregated, walk
 * every player who has stats for that matchday and update their global
 * {@code marketValue} using {@link MarketValueCalculator}.
 *
 * Inputs are sourced from real stored data only:
 *   - lastMatchdayPoints  -> MAX of player_matchday_scores.points for this matchday
 *                           (same value across leagues; MAX collapses duplicates).
 *   - averagePoints       -> AVG of player_matchday_scores.points across every
 *                           matchday whose number is STRICTLY LESS than this one.
 *   - minutesPlayed       -> SUM of player_match_stats.minutes_played across
 *                           the FINISHED real matches of this matchday.
 *   - didNotPlay          -> true if no stats row exists OR every stats row
 *                           has did_not_play = true.
 *   - availability        -> player.availabilityStatus (default AVAILABLE).
 *   - team elimination /
 *     reachedSemi/Final   -> NEUTRAL FALLBACK (false) until a bracket model
 *                           exists; the resolver hook is in place for later.
 *   - importance          -> player.importance (nullable - calculator handles it).
 *
 * Idempotency: each (player, matchday) writes at most one history row. A
 * second call to {@link #applyForMatchday(UUID)} for the same matchday is a
 * no-op for any player that already has a history entry tagged with that
 * matchday.
 *
 * Side effects per updated player:
 *   - player.marketValue and player.currentValue are bumped.
 *   - player.initialMarketValue is NEVER touched.
 *   - one MarketValueHistory row written (with matchdayId).
 *   - every SquadPlayerEntity referencing this player has its effective
 *     release clause recomputed via {@link ReleaseClauseService#recalculate}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicMarketValueService {

    private final MarketValueCalculator calculator;
    private final MarketValueHistoryRepository historyRepository;
    private final PlayerRepository playerRepository;
    private final MatchdayRepository matchdayRepository;
    private final RealMatchRepository realMatchRepository;
    private final PlayerMatchStatsRepository statsRepository;
    private final PlayerMatchdayScoreRepository playerScoreRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final ReleaseClauseService releaseClauseService;
    private final ObjectMapper objectMapper;

    public record ApplyResult(UUID matchdayId, int playersConsidered,
                              int playersUpdated, int playersSkippedIdempotent) {}

    @Transactional
    public ApplyResult applyForMatchday(UUID matchdayId) {
        MatchdayEntity matchday = matchdayRepository.findById(matchdayId)
                .orElseThrow(() -> new NotFoundException("Matchday not found: " + matchdayId));

        // --- Source 1: stats (covers EVERY player who played a real match in this matchday) ---
        List<RealMatchEntity> realMatches = realMatchRepository.findByMatchdayId(matchdayId);
        if (realMatches.isEmpty()) {
            log.info("Dynamic market-value update for matchday {} skipped: no real matches.", matchdayId);
            return new ApplyResult(matchdayId, 0, 0, 0);
        }
        List<UUID> realMatchIds = realMatches.stream().map(RealMatchEntity::getId).toList();
        List<PlayerMatchStatsEntity> allStats = statsRepository.findByRealMatchIdIn(realMatchIds);

        Map<UUID, List<PlayerMatchStatsEntity>> statsByPlayer = new HashMap<>();
        for (var s : allStats) {
            statsByPlayer.computeIfAbsent(s.getPlayerId(), k -> new java.util.ArrayList<>()).add(s);
        }

        // --- Source 2: persisted matchday points (per spec) ---
        Map<UUID, Integer> pointsByPlayer = new HashMap<>();
        for (var row : playerScoreRepository.findPointsByMatchday(matchdayId)) {
            pointsByPlayer.put(row.getPlayerId(), row.getPoints());
        }

        // --- Source 3: historical averages ---
        Map<UUID, Double> avgByPlayer = new HashMap<>();
        for (var row : playerScoreRepository.findHistoricalAveragesBefore(matchday.getNumber())) {
            avgByPlayer.put(row.getPlayerId(), row.getAvgPoints());
        }

        // The universe of players to consider: anyone who has stats for this matchday.
        // Players not in any user's squad still get a value update so the market keeps moving.
        Set<UUID> playerIds = new HashSet<>(statsByPlayer.keySet());

        int updated = 0;
        int skipped = 0;
        for (UUID playerId : playerIds) {
            if (historyRepository.existsByPlayerIdAndMatchdayId(playerId, matchdayId)) {
                skipped++;
                continue;
            }
            PlayerEntity player = playerRepository.findById(playerId).orElse(null);
            if (player == null) continue;

            List<PlayerMatchStatsEntity> playerStats = statsByPlayer.get(playerId);
            int minutesPlayed = playerStats.stream().mapToInt(PlayerMatchStatsEntity::getMinutesPlayed).sum();
            boolean didNotPlay = playerStats.stream().allMatch(PlayerMatchStatsEntity::isDidNotPlay);
            Integer lastPoints = pointsByPlayer.get(playerId); // may be null if no league had him
            Double avgPoints = avgByPlayer.get(playerId);      // null on matchday 1

            DynamicValueInput input = DynamicValueInput.builder()
                    .currentMarketValue(player.getMarketValue())
                    .importance(player.getImportance())
                    .availability(player.getAvailabilityStatus())
                    .lastMatchdayPoints(lastPoints)
                    .averagePoints(avgPoints)
                    .minutesPlayed(minutesPlayed)
                    .didNotPlay(didNotPlay)
                    .restedSuperstar(false)
                    // Tournament context unmodeled yet -> neutral fallback (spec allows).
                    .teamEliminated(false)
                    .teamReachedSemiFinals(false)
                    .teamReachedFinal(false)
                    .demandScore(0)
                    .build();

            MarketValueResult result = calculator.calculateDelta(input);

            // Write history regardless of delta (matchdayId is the idempotency key).
            historyRepository.save(MarketValueHistoryEntity.builder()
                    .playerId(player.getId())
                    .oldValue(result.oldValue())
                    .newValue(result.newValue())
                    .delta(result.delta())
                    .deltaPercent(result.deltaPercent())
                    .momentumScore(result.momentumScore())
                    .reason(result.reason())
                    .matchdayId(matchdayId)
                    .breakdownJson(toJson(result.breakdown()))
                    .build());

            if (result.delta().signum() != 0) {
                player.setMarketValue(result.newValue());
                player.setCurrentValue(result.newValue()); // legacy mirror, never touch initialMarketValue
                playerRepository.save(player);
                recalculateClausesFor(player);
            }
            updated++;
        }

        log.info("Dynamic market-value update for matchday {}: considered={}, updated={}, idempotent skips={}.",
                matchdayId, playerIds.size(), updated, skipped);
        return new ApplyResult(matchdayId, playerIds.size(), updated, skipped);
    }

    private void recalculateClausesFor(PlayerEntity player) {
        List<SquadPlayerEntity> ownerships = squadPlayerRepository.findByPlayerId(player.getId());
        for (SquadPlayerEntity sp : ownerships) {
            releaseClauseService.recalculate(sp);
            squadPlayerRepository.save(sp);
        }
    }

    private String toJson(Object o) {
        if (o == null) return null;
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { return null; }
    }
}
