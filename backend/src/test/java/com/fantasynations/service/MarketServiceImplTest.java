package com.fantasynations.service;

import com.fantasynations.domain.LeagueRules;
import com.fantasynations.domain.MarketCycleStatus;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.dto.MarketResponseDto;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.MarketPlayerEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.market.MarketCycleService;
import com.fantasynations.market.MarketListingPopulator;
import com.fantasynations.repository.BidRepository;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.MarketPlayerRepository;
import com.fantasynations.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketServiceImplTest {

    private MarketPlayerRepository marketPlayerRepository;
    private LeagueRepository leagueRepository;
    private LeagueMemberRepository leagueMemberRepository;
    private BidRepository bidRepository;
    private MarketCycleService cycleService;
    private MarketListingPopulator populator;
    private MarketServiceImpl service;

    private UUID leagueId;
    private UUID userId;
    private LeagueEntity league;
    private MarketCycleEntity openCycle;
    private final List<MarketPlayerEntity> listings = new ArrayList<>();

    @BeforeEach
    void setUp() {
        marketPlayerRepository = mock(MarketPlayerRepository.class);
        leagueRepository = mock(LeagueRepository.class);
        leagueMemberRepository = mock(LeagueMemberRepository.class);
        bidRepository = mock(BidRepository.class);
        cycleService = mock(MarketCycleService.class);
        populator = mock(MarketListingPopulator.class);

        service = new MarketServiceImpl(
                marketPlayerRepository, leagueRepository, leagueMemberRepository,
                bidRepository, cycleService, populator, mock(UserRepository.class));

        leagueId = UUID.randomUUID();
        userId = UUID.randomUUID();
        league = LeagueEntity.builder()
                .id(leagueId).name("Test League").rules(new LeagueRules()).build();
        openCycle = MarketCycleEntity.builder()
                .id(UUID.randomUUID()).leagueId(leagueId).cycleNumber(1)
                .opensAt(LocalDateTime.now())
                .closesAt(LocalDateTime.now().plusHours(24))
                .status(MarketCycleStatus.OPEN).build();

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)).thenReturn(true);
        when(cycleService.getOrCreateOpenCycle(leagueId)).thenReturn(openCycle);
        when(marketPlayerRepository.findByCycleId(openCycle.getId()))
                .thenAnswer(inv -> new ArrayList<>(listings));
        when(bidRepository.findByMarketPlayerIdAndUserId(any(), any()))
                .thenReturn(Optional.empty());
        // Default populator behaviour: no-op. Tests opt-in to populator
        // appending listings via stubPopulatorToAdd(...). Stubbing the
        // side-effect in setUp leaks across tests that need an empty pool.
    }

    /** Stubs populator to append {@code n} listings the first time it runs. */
    private void stubPopulatorToAdd(int n) {
        when(populator.populateForCycle(openCycle)).thenAnswer(inv -> {
            listings.addAll(fakeListings(n));
            return n;
        });
    }

    @Test
    void initializeMarketPopulatesWhenCycleIsEmpty() {
        stubPopulatorToAdd(15);
        service.initializeMarketIfMissing(leagueId);
        verify(populator, times(1)).populateForCycle(openCycle);
        assertThat(listings).hasSize(15);
    }

    @Test
    void initializeMarketIsIdempotent() {
        stubPopulatorToAdd(15);
        service.initializeMarketIfMissing(leagueId);
        service.initializeMarketIfMissing(leagueId);
        verify(populator, times(1)).populateForCycle(openCycle);
    }

    @Test
    void getMarketReturnsListingsWithCycleCloseTimeAsNextRefresh() {
        stubPopulatorToAdd(15);
        MarketResponseDto response = service.getMarket(leagueId, userId);

        assertThat(response.available()).isTrue();
        assertThat(response.players()).hasSize(15);
        assertThat(response.nextRefreshAt()).isEqualTo(openCycle.getClosesAt());
        assertThat(response.reason()).isNull();
    }

    @Test
    void getMarketReturnsNoPoolInPoolWhenEmpty() {
        // populator stub returns 0 - simulate by replacing it
        when(populator.populateForCycle(openCycle)).thenReturn(0);

        MarketResponseDto response = service.getMarket(leagueId, userId);

        assertThat(response.players()).isEmpty();
        assertThat(response.reason()).isEqualTo(MarketServiceImpl.REASON_NO_PLAYERS_IN_POOL);
    }

    @Test
    void getMarketReturnsNotEnoughPlayersWhenPartial() {
        stubPopulatorToAdd(5); // rules ask for 15 by default
        MarketResponseDto response = service.getMarket(leagueId, userId);

        assertThat(response.players()).hasSize(5);
        assertThat(response.reason()).isEqualTo(MarketServiceImpl.REASON_NOT_ENOUGH_PLAYERS);
    }

    @Test
    void getMarketForbiddenForNonMember() {
        when(leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)).thenReturn(false);
        assertThatThrownBy(() -> service.getMarket(leagueId, userId))
                .isInstanceOf(ForbiddenException.class);
        verify(populator, never()).populateForCycle(any());
    }

    private List<MarketPlayerEntity> fakeListings(int n) {
        var out = new ArrayList<MarketPlayerEntity>();
        for (int i = 0; i < n; i++) {
            PlayerEntity p = PlayerEntity.builder()
                    .id(UUID.randomUUID()).name("P" + i).nationalTeam("T")
                    .position(PlayerPosition.MID)
                    .baseValue(BigDecimal.ONE).currentValue(BigDecimal.ONE)
                    .initialMarketValue(BigDecimal.ONE).marketValue(BigDecimal.ONE)
                    .active(true).build();
            out.add(MarketPlayerEntity.builder()
                    .id(UUID.randomUUID()).league(league).player(p)
                    .price(BigDecimal.ONE).availableUntil(openCycle.getClosesAt())
                    .cycleId(openCycle.getId())
                    .build());
        }
        return out;
    }
}
