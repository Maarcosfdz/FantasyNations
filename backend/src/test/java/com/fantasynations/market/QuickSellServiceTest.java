package com.fantasynations.market;

import com.fantasynations.domain.LeagueRole;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.LeagueMemberEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.SquadEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.marketvalue.MarketValueConfig;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import com.fantasynations.service.ActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuickSellServiceTest {

    private SquadPlayerRepository squadPlayerRepository;
    private LeagueMemberRepository leagueMemberRepository;
    private LeagueRepository leagueRepository;
    private ActivityLogService activityLogService;
    private QuickSellService service;

    private UUID leagueId;
    private UUID userId;
    private LeagueEntity league;
    private LeagueMemberEntity member;
    private SquadPlayerEntity squadPlayer;

    @BeforeEach
    void setUp() {
        squadPlayerRepository = mock(SquadPlayerRepository.class);
        leagueMemberRepository = mock(LeagueMemberRepository.class);
        leagueRepository = mock(LeagueRepository.class);
        activityLogService = mock(ActivityLogService.class);
        service = new QuickSellService(squadPlayerRepository, leagueMemberRepository,
                leagueRepository, new MarketValueConfig(), activityLogService);

        leagueId = UUID.randomUUID();
        userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).build();
        league = LeagueEntity.builder().id(leagueId).name("L")
                .rules(new com.fantasynations.domain.LeagueRules()).build();
        SquadEntity squad = SquadEntity.builder()
                .id(UUID.randomUUID()).league(league).user(user).build();
        PlayerEntity player = PlayerEntity.builder()
                .id(UUID.randomUUID()).name("P").nationalTeam("Spain")
                .position(PlayerPosition.MID)
                .marketValue(new BigDecimal("10000000"))
                .initialMarketValue(new BigDecimal("10000000"))
                .baseValue(new BigDecimal("10000000"))
                .currentValue(new BigDecimal("10000000"))
                .active(true).build();
        squadPlayer = SquadPlayerEntity.builder()
                .id(UUID.randomUUID()).squad(squad).player(player)
                .releaseClause(BigDecimal.ZERO).build();
        member = LeagueMemberEntity.builder()
                .id(UUID.randomUUID()).league(league).user(user)
                .role(LeagueRole.MEMBER)
                .money(new BigDecimal("100000000")).build();

        when(squadPlayerRepository.findById(squadPlayer.getId()))
                .thenReturn(Optional.of(squadPlayer));
        when(leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId))
                .thenReturn(Optional.of(member));
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void payoutIsFiftyPercentOfMarketValue() {
        var result = service.quickSell(leagueId, squadPlayer.getId(), userId);

        assertThat(result.amountCredited()).isEqualByComparingTo(new BigDecimal("5000000"));
        assertThat(member.getMoney()).isEqualByComparingTo(new BigDecimal("105000000"));
        verify(squadPlayerRepository).delete(squadPlayer);
        verify(activityLogService).log(any(), any(), any(), any());
    }

    @Test
    void cannotQuickSellAPlayerYouDoNotOwn() {
        UUID otherUser = UUID.randomUUID();
        assertThatThrownBy(() -> service.quickSell(leagueId, squadPlayer.getId(), otherUser))
                .isInstanceOf(ForbiddenException.class);
    }
}
