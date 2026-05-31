package com.fantasynations.scoring;

import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.PlayerMatchStatsEntity;
import com.fantasynations.scoring.dto.MatchEvents;
import com.fantasynations.scoring.dto.OptionalStats;
import com.fantasynations.scoring.dto.PerformanceStats;
import com.fantasynations.scoring.dto.ShootoutEvents;
import org.springframework.stereotype.Component;

@Component
public class PlayerMatchStatsMapper {

    public PerformanceStats toPerformanceStats(PlayerPosition position, PlayerMatchStatsEntity e) {
        return PerformanceStats.builder()
                .position(position)
                .minutesPlayed(e.getMinutesPlayed())
                .didNotPlay(e.isDidNotPlay())
                .onPitchGoalsConceded(e.getOnPitchGoalsConceded())
                .teamCleanSheet(e.isTeamCleanSheet())
                .events(new MatchEvents(
                        e.getGoals(),
                        e.getPenaltyGoals(),
                        e.getAssists(),
                        e.getBigChancesCreated(),
                        e.getPenaltiesWon(),
                        e.getPenaltiesConceded(),
                        e.getPenaltiesMissed(),
                        e.getPenaltiesSavedByGk(),
                        e.getSaves(),
                        e.getYellowCards(),
                        e.getDoubleYellows(),
                        e.getDirectReds(),
                        e.getOwnGoals()
                ))
                .shootout(new ShootoutEvents(
                        e.getShootoutGoals(),
                        e.getShootoutMisses(),
                        e.getShootoutSavesByGk()
                ))
                .optional(new OptionalStats(
                        e.getShotsOnTarget(),
                        e.getSuccessfulDribbles(),
                        e.getKeyPasses(),
                        e.getDuelsWonPlusInterceptions(),
                        e.getClearances(),
                        e.getBigChancesMissed(),
                        e.getErrorLeadingToGoal()
                ))
                .build();
    }
}
