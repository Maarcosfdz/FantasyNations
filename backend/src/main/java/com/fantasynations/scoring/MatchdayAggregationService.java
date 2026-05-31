package com.fantasynations.scoring;

import com.fantasynations.domain.LeagueRules;
import com.fantasynations.domain.MatchdayAggregationReason;
import com.fantasynations.entity.LineupEntity;
import com.fantasynations.entity.LockedLineupPlayerEntity;
import com.fantasynations.entity.MatchdayEntity;
import com.fantasynations.entity.MatchdayScoreEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.PlayerMatchStatsEntity;
import com.fantasynations.entity.PlayerMatchdayScoreEntity;
import com.fantasynations.entity.RealMatchEntity;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.LineupRepository;
import com.fantasynations.repository.MatchdayRepository;
import com.fantasynations.repository.MatchdayScoreRepository;
import com.fantasynations.repository.PlayerMatchStatsRepository;
import com.fantasynations.repository.PlayerMatchdayScoreRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fantasynations.repository.RealMatchRepository;
import com.fantasynations.scoring.dto.MatchdayEligibility;
import com.fantasynations.scoring.dto.PerformanceStats;
import com.fantasynations.scoring.dto.ScoreBreakdown;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregates a user's matchday score from the frozen lineup snapshot. The
 * snapshot, not the live lineup, is the single source of truth for a
 * matchday's score.
 *
 * Rules:
 *   - lineup incomplete (< {@code minLineupPlayers}) at first aggregation -> total = 0, reason INCOMPLETE_LINEUP
 *   - user balance < 0 at first aggregation                              -> total = 0, reason NEGATIVE_BALANCE
 *   - eligibility filter (eliminated, not qualified, matchday-7 slot) skips per-player points
 *   - re-aggregating reuses the existing snapshot (no double-freeze)
 *   - persisted MatchdayScore + PlayerMatchdayScore rows are overwritten on re-aggregation
 *     (totals can change if stats are corrected)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchdayAggregationService {

    private final LineupFreezeService freezeService;
    private final FantasyScoringService scoringService;
    private final PlayerMatchStatsMapper statsMapper;
    private final MatchdayRepository matchdayRepository;
    private final RealMatchRepository realMatchRepository;
    private final LineupRepository lineupRepository;
    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final MatchdayScoreRepository matchdayScoreRepository;
    private final PlayerMatchdayScoreRepository playerScoreRepository;
    private final PlayerMatchStatsRepository statsRepository;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;

    public record AggregationResult(
            UUID matchdayScoreId,
            int totalPoints,
            MatchdayAggregationReason reason,
            int snapshotSize
    ) {}

    /** Convenience for the scheduled job - aggregates every user in the league. */
    @Transactional
    public List<AggregationResult> aggregateAllMembers(UUID leagueId, UUID matchdayId) {
        var members = leagueMemberRepository.findByLeagueId(leagueId);
        return members.stream()
                .map(m -> aggregate(leagueId, m.getUser().getId(), matchdayId))
                .toList();
    }

    @Transactional
    public AggregationResult aggregate(UUID leagueId, UUID userId, UUID matchdayId) {
        MatchdayEntity matchday = matchdayRepository.findById(matchdayId)
                .orElseThrow(() -> new NotFoundException("Matchday not found: " + matchdayId));
        LeagueRules rules = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new NotFoundException("League not found: " + leagueId))
                .getRules();

        // 1) Snapshot the lineup (or reuse the existing one).
        Optional<LineupEntity> maybeLineup = lineupRepository.findByLeagueIdAndUserId(leagueId, userId);
        List<LockedLineupPlayerEntity> snapshot = maybeLineup
                .map(l -> freezeService.getOrFreeze(l, matchdayId))
                .orElse(List.of());

        // 2) Hard zero rules.
        if (snapshot.size() < rules.getMinLineupPlayers()) {
            return persist(leagueId, userId, matchdayId, 0,
                    MatchdayAggregationReason.INCOMPLETE_LINEUP, List.of());
        }
        var member = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId).orElseThrow();
        if (member.getMoney().compareTo(BigDecimal.ZERO) < 0) {
            return persist(leagueId, userId, matchdayId, 0,
                    MatchdayAggregationReason.NEGATIVE_BALANCE, List.of());
        }

        // 3) Build the real-match -> stats index once. Only FINISHED matches contribute.
        List<RealMatchEntity> realMatches = realMatchRepository.findByMatchdayId(matchdayId).stream()
                .filter(rm -> rm.getStatus() == com.fantasynations.domain.RealMatchStatus.FINISHED)
                .toList();
        Map<UUID, List<PlayerMatchStatsEntity>> statsByPlayer = new HashMap<>();
        for (var rm : realMatches) {
            for (var stat : statsRepository.findByRealMatchId(rm.getId())) {
                statsByPlayer.computeIfAbsent(stat.getPlayerId(), k -> new java.util.ArrayList<>()).add(stat);
            }
        }

        int total = 0;
        List<PlayerScoreRow> rows = new java.util.ArrayList<>();
        for (var locked : snapshot) {
            PlayerEntity player = playerRepository.findById(locked.getPlayerId()).orElse(null);
            if (player == null) {
                rows.add(new PlayerScoreRow(locked.getPlayerId(), locked.getPositionSlot(), 0, null));
                continue;
            }
            MatchdayEligibility eligibility = resolveEligibility(player, matchday);
            if (!eligibility.canScore(matchday.getNumber())) {
                rows.add(new PlayerScoreRow(player.getId(), locked.getPositionSlot(), 0, breakdownJson(Map.of("eligibility", 0))));
                continue;
            }

            int playerTotal = 0;
            Map<String, Integer> aggregatedBreakdown = new HashMap<>();
            for (PlayerMatchStatsEntity stat : statsByPlayer.getOrDefault(player.getId(), List.of())) {
                PerformanceStats perf = statsMapper.toPerformanceStats(player.getPosition(), stat);
                ScoreBreakdown sb = scoringService.calculate(perf);
                playerTotal += sb.total();
                sb.byCategory().forEach((k, v) -> aggregatedBreakdown.merge(k, v, Integer::sum));
            }
            rows.add(new PlayerScoreRow(player.getId(), locked.getPositionSlot(), playerTotal,
                    breakdownJson(aggregatedBreakdown)));
            total += playerTotal;
        }

        return persist(leagueId, userId, matchdayId, total, MatchdayAggregationReason.OK, rows);
    }

    /**
     * Tournament-context resolver. For now this is permissive: every player is
     * treated as eligible. The actual elimination / qualification / matchday-7
     * data will be wired up when the bracket is modelled.
     */
    private MatchdayEligibility resolveEligibility(PlayerEntity player, MatchdayEntity matchday) {
        return MatchdayEligibility.eligible();
    }

    // --- persistence helpers ---------------------------------------------

    private record PlayerScoreRow(UUID playerId, String positionSlot, int points, String breakdownJson) {}

    private AggregationResult persist(UUID leagueId, UUID userId, UUID matchdayId, int total,
                                       MatchdayAggregationReason reason, List<PlayerScoreRow> rows) {
        var existing = matchdayScoreRepository
                .findByLeagueIdAndUserIdAndMatchdayId(leagueId, userId, matchdayId)
                .orElseGet(() -> MatchdayScoreEntity.builder()
                        .leagueId(leagueId).userId(userId).matchdayId(matchdayId).build());
        existing.setTotalPoints(total);
        existing.setReason(reason);
        existing.setAggregatedAt(java.time.LocalDateTime.now());
        var saved = matchdayScoreRepository.save(existing);

        // Replace per-player rows.
        playerScoreRepository.deleteByMatchdayScoreId(saved.getId());
        for (var row : rows) {
            playerScoreRepository.save(PlayerMatchdayScoreEntity.builder()
                    .matchdayScoreId(saved.getId())
                    .playerId(row.playerId())
                    .positionSlot(row.positionSlot())
                    .points(row.points())
                    .breakdownJson(row.breakdownJson())
                    .build());
        }
        log.info("Aggregated matchday {} for user {} in league {}: {} points ({}).",
                matchdayId, userId, leagueId, total, reason);
        return new AggregationResult(saved.getId(), total, reason, rows.size());
    }

    private String breakdownJson(Map<String, Integer> breakdown) {
        if (breakdown == null || breakdown.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(breakdown);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
