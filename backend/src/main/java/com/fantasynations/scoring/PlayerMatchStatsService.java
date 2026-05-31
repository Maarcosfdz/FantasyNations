package com.fantasynations.scoring;

import com.fantasynations.dto.PlayerMatchStatsRequestDto;
import com.fantasynations.entity.PlayerMatchStatsEntity;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.PlayerMatchStatsRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fantasynations.repository.RealMatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Upserts per-player per-match stats by (player, real_match). Re-posting the
 * same player updates the existing row.
 */
@Service
@RequiredArgsConstructor
public class PlayerMatchStatsService {

    private final PlayerMatchStatsRepository statsRepository;
    private final RealMatchRepository realMatchRepository;
    private final PlayerRepository playerRepository;

    @Transactional
    public PlayerMatchStatsEntity upsert(UUID realMatchId, PlayerMatchStatsRequestDto dto) {
        realMatchRepository.findById(realMatchId)
                .orElseThrow(() -> new NotFoundException("Real match not found: " + realMatchId));
        playerRepository.findById(dto.playerId())
                .orElseThrow(() -> new NotFoundException("Player not found: " + dto.playerId()));

        PlayerMatchStatsEntity entity = statsRepository
                .findByPlayerIdAndRealMatchId(dto.playerId(), realMatchId)
                .orElseGet(() -> PlayerMatchStatsEntity.builder()
                        .playerId(dto.playerId())
                        .realMatchId(realMatchId)
                        .build());

        entity.setDidNotPlay(dto.didNotPlay());
        entity.setMinutesPlayed(dto.minutesPlayed());
        entity.setOnPitchGoalsConceded(dto.onPitchGoalsConceded());
        entity.setTeamCleanSheet(dto.teamCleanSheet());

        entity.setGoals(dto.goals());
        entity.setPenaltyGoals(dto.penaltyGoals());
        entity.setAssists(dto.assists());
        entity.setBigChancesCreated(dto.bigChancesCreated());
        entity.setPenaltiesWon(dto.penaltiesWon());
        entity.setPenaltiesConceded(dto.penaltiesConceded());
        entity.setPenaltiesMissed(dto.penaltiesMissed());
        entity.setPenaltiesSavedByGk(dto.penaltiesSavedByGk());
        entity.setSaves(dto.saves());
        entity.setYellowCards(dto.yellowCards());
        entity.setDoubleYellows(dto.doubleYellows());
        entity.setDirectReds(dto.directReds());
        entity.setOwnGoals(dto.ownGoals());

        entity.setShootoutGoals(dto.shootoutGoals());
        entity.setShootoutMisses(dto.shootoutMisses());
        entity.setShootoutSavesByGk(dto.shootoutSavesByGk());

        entity.setShotsOnTarget(dto.shotsOnTarget());
        entity.setSuccessfulDribbles(dto.successfulDribbles());
        entity.setKeyPasses(dto.keyPasses());
        entity.setDuelsWonPlusInterceptions(dto.duelsWonPlusInterceptions());
        entity.setClearances(dto.clearances());
        entity.setBigChancesMissed(dto.bigChancesMissed());
        entity.setErrorLeadingToGoal(dto.errorLeadingToGoal());

        return statsRepository.save(entity);
    }
}
