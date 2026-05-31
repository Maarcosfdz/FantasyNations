package com.fantasynations.scoring;

import com.fantasynations.dto.MatchdayScoreResponseDto;
import com.fantasynations.dto.MatchdayScoreResponseDto.PlayerScoreDto;
import com.fantasynations.entity.MatchdayScoreEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.MatchdayRepository;
import com.fantasynations.repository.MatchdayScoreRepository;
import com.fantasynations.repository.PlayerMatchdayScoreRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Read-only API for the frontend "Matchday Scores / Puntuaciones" view.
 * Returns the user's frozen matchday lineup with per-player points and
 * per-category breakdown.
 */
@Service
@RequiredArgsConstructor
public class MatchdayScoreReader {

    private final MatchdayScoreRepository matchdayScoreRepository;
    private final PlayerMatchdayScoreRepository playerScoreRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final MatchdayRepository matchdayRepository;
    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public MatchdayScoreResponseDto getForUser(UUID leagueId, UUID matchdayId, UUID viewerUserId) {
        return getForUser(leagueId, matchdayId, viewerUserId, viewerUserId);
    }

    @Transactional(readOnly = true)
    public MatchdayScoreResponseDto getForUser(
            UUID leagueId, UUID matchdayId, UUID viewerUserId, UUID targetUserId) {
        if (!leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, viewerUserId)) {
            throw new ForbiddenException("Not a member of this league");
        }
        var matchday = matchdayRepository.findById(matchdayId)
                .orElseThrow(() -> new NotFoundException("Matchday not found"));

        MatchdayScoreEntity scoreEntity = matchdayScoreRepository
                .findByLeagueIdAndUserIdAndMatchdayId(leagueId, targetUserId, matchdayId)
                .orElse(null);

        if (scoreEntity == null) {
            // No aggregation yet for this user/matchday.
            return new MatchdayScoreResponseDto(
                    matchdayId, matchday.getNumber(), leagueId, targetUserId,
                    0, null, null, List.of());
        }

        var rows = playerScoreRepository.findByMatchdayScoreId(scoreEntity.getId());
        Map<UUID, PlayerEntity> playerById = playerRepository.findAllById(
                rows.stream().map(r -> r.getPlayerId()).toList())
                .stream().collect(Collectors.toMap(PlayerEntity::getId, p -> p));

        List<PlayerScoreDto> players = rows.stream().map(r -> {
            PlayerEntity p = playerById.get(r.getPlayerId());
            return new PlayerScoreDto(
                    r.getPlayerId(),
                    p != null ? p.getName() : "",
                    p != null ? p.getNationalTeam() : "",
                    p != null ? p.getPosition().name() : "",
                    r.getPositionSlot(),
                    p != null ? p.getImageRef() : null,
                    r.getPoints(),
                    parseBreakdown(r.getBreakdownJson())
            );
        }).toList();

        return new MatchdayScoreResponseDto(
                matchdayId,
                matchday.getNumber(),
                leagueId,
                targetUserId,
                scoreEntity.getTotalPoints(),
                scoreEntity.getReason(),
                scoreEntity.getAggregatedAt(),
                players
        );
    }

    private Map<String, Integer> parseBreakdown(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            return new HashMap<>();
        }
    }
}
