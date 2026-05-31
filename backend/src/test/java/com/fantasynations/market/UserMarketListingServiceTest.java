package com.fantasynations.market;

import com.fantasynations.domain.MarketCycleStatus;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.MarketPlayerEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.SquadEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.repository.MarketPlayerRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserMarketListingServiceTest {

    private SquadPlayerRepository squadPlayerRepository;
    private MarketPlayerRepository marketPlayerRepository;
    private MarketCycleService cycleService;
    private UserMarketListingService service;

    private UUID leagueId;
    private UUID userId;
    private LeagueEntity league;
    private MarketCycleEntity cycle;
    private SquadPlayerEntity squadPlayer;

    @BeforeEach
    void setUp() {
        squadPlayerRepository = mock(SquadPlayerRepository.class);
        marketPlayerRepository = mock(MarketPlayerRepository.class);
        cycleService = mock(MarketCycleService.class);
        service = new UserMarketListingService(
                squadPlayerRepository, marketPlayerRepository, cycleService);

        leagueId = UUID.randomUUID();
        userId = UUID.randomUUID();
        league = LeagueEntity.builder().id(leagueId).name("L").build();
        UserEntity owner = UserEntity.builder().id(userId).build();
        cycle = MarketCycleEntity.builder()
                .id(UUID.randomUUID()).leagueId(leagueId).cycleNumber(1)
                .opensAt(LocalDateTime.now())
                .closesAt(LocalDateTime.now().plusHours(24))
                .status(MarketCycleStatus.OPEN).build();

        PlayerEntity player = PlayerEntity.builder()
                .id(UUID.randomUUID()).name("P").nationalTeam("Spain")
                .position(PlayerPosition.FWD)
                .baseValue(BigDecimal.ONE).currentValue(BigDecimal.ONE)
                .initialMarketValue(BigDecimal.ONE).marketValue(BigDecimal.ONE)
                .active(true).build();
        SquadEntity squad = SquadEntity.builder()
                .id(UUID.randomUUID()).league(league).user(owner).build();
        squadPlayer = SquadPlayerEntity.builder()
                .id(UUID.randomUUID()).squad(squad).player(player)
                .releaseClause(BigDecimal.ZERO).build();

        when(squadPlayerRepository.findById(squadPlayer.getId()))
                .thenReturn(Optional.of(squadPlayer));
        when(cycleService.getOrCreateOpenCycle(leagueId)).thenReturn(cycle);
        when(marketPlayerRepository.save(any())).thenAnswer(inv -> {
            MarketPlayerEntity mp = inv.getArgument(0);
            if (mp.getId() == null) mp.setId(UUID.randomUUID());
            return mp;
        });
    }

    @Test
    void createsListingWithSellerAnd48hAvailableUntil() {
        BigDecimal price = new BigDecimal("12000000");
        LocalDateTime before = LocalDateTime.now();

        MarketPlayerEntity listing = service.listOnMarket(
                leagueId, squadPlayer.getId(), userId, price);

        assertThat(listing.getSellerUserId()).isEqualTo(userId);
        assertThat(listing.getPrice()).isEqualByComparingTo(price);
        assertThat(listing.getCycleId()).isEqualTo(cycle.getId());
        long hours = Duration.between(before, listing.getAvailableUntil()).toHours();
        assertThat(hours).isBetween(47L, 49L); // ~48h
    }

    @Test
    void cannotListAnotherUsersPlayer() {
        UUID other = UUID.randomUUID();
        assertThatThrownBy(() -> service.listOnMarket(leagueId, squadPlayer.getId(), other,
                new BigDecimal("10000000")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cannotListWithNonPositivePrice() {
        assertThatThrownBy(() -> service.listOnMarket(leagueId, squadPlayer.getId(), userId, BigDecimal.ZERO))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.listOnMarket(leagueId, squadPlayer.getId(), userId, new BigDecimal("-1")))
                .isInstanceOf(BadRequestException.class);
    }
}
