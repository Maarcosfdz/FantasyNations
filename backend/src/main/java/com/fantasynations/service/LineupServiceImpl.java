package com.fantasynations.service;

import com.fantasynations.domain.ActivityEventType;
import com.fantasynations.dto.LineupPlayerDto;
import com.fantasynations.entity.*;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.*;
import com.fantasynations.validation.LineupValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LineupServiceImpl implements LineupService {

    private final LineupRepository lineupRepository;
    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerRepository playerRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final LeagueRepository leagueRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final LineupValidator lineupValidator;

    @Override
    @Transactional(readOnly = true)
    public List<LineupPlayerDto> getLineup(UUID leagueId, UUID userId) {
        if (!leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)) {
            throw new ForbiddenException("Not a member of this league");
        }
        return lineupRepository.findByLeagueIdAndUserId(leagueId, userId)
                .map(lineup -> lineup.getPlayers().stream()
                        .map(lp -> new LineupPlayerDto(
                                lp.getPlayer().getId(), lp.getPlayer().getName(),
                                lp.getPlayer().getNationalTeam(), lp.getPlayer().getPosition(),
                                lp.getPlayer().getImageRef(), lp.getPositionSlot()))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    @Override
    @Transactional
    public void saveLineup(UUID leagueId, UUID userId, Map<UUID, String> playerSlotMap) {
        if (!leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)) {
            throw new ForbiddenException("Not a member of this league");
        }
        var squad = squadRepository.findByLeagueIdAndUserId(leagueId, userId)
                .orElseThrow(() -> new NotFoundException("Squad not found"));

        lineupValidator.validate(playerSlotMap, squad.getId());

        var league = leagueRepository.findById(leagueId).orElseThrow();
        var user = userRepository.findById(userId).orElseThrow();

        var lineup = lineupRepository.findByLeagueIdAndUserId(leagueId, userId)
                .orElseGet(() -> {
                    var newLineup = LineupEntity.builder()
                            .league(league)
                            .user(user)
                            .build();
                    return lineupRepository.save(newLineup);
                });

        lineup.getPlayers().clear();

        playerSlotMap.forEach((playerId, slot) -> {
            var player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new NotFoundException("Player not found: " + playerId));
            var lp = LineupPlayerEntity.builder()
                    .lineup(lineup)
                    .player(player)
                    .positionSlot(slot)
                    .build();
            lineup.getPlayers().add(lp);
        });

        lineupRepository.save(lineup);
        activityLogService.log(league, user, ActivityEventType.LINEUP_CHANGED, Map.of());
    }
}
