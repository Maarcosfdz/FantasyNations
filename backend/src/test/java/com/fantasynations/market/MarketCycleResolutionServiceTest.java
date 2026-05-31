package com.fantasynations.market;

import com.fantasynations.domain.BidStatus;
import com.fantasynations.domain.LeagueRole;
import com.fantasynations.domain.MarketCycleStatus;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.BidEntity;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.LeagueMemberEntity;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.MarketPlayerEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.SquadEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.marketvalue.MarketValueConfig;
import com.fantasynations.marketvalue.ReleaseClauseService;
import com.fantasynations.repository.BidRepository;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.MachineOfferRepository;
import com.fantasynations.repository.MarketCycleRepository;
import com.fantasynations.repository.MarketPlayerRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import com.fantasynations.repository.SquadRepository;
import com.fantasynations.repository.UserRepository;
import com.fantasynations.service.ActivityLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketCycleResolutionServiceTest {

    private MarketCycleRepository cycleRepository;
    private MarketPlayerRepository marketPlayerRepository;
    private BidRepository bidRepository;
    private MachineOfferRepository machineOfferRepository;
    private LeagueMemberRepository leagueMemberRepository;
    private SquadRepository squadRepository;
    private SquadPlayerRepository squadPlayerRepository;
    private UserRepository userRepository;
    private MarketCycleService cycleService;
    private MarketListingPopulator listingPopulator;
    private MarketCycleResolutionService service;

    private LeagueEntity league;
    private MarketCycleEntity cycle;
    private final Map<UUID, LeagueMemberEntity> members = new HashMap<>();

    @BeforeEach
    void setUp() {
        cycleRepository = mock(MarketCycleRepository.class);
        marketPlayerRepository = mock(MarketPlayerRepository.class);
        bidRepository = mock(BidRepository.class);
        machineOfferRepository = mock(MachineOfferRepository.class);
        leagueMemberRepository = mock(LeagueMemberRepository.class);
        squadRepository = mock(SquadRepository.class);
        squadPlayerRepository = mock(SquadPlayerRepository.class);
        userRepository = mock(UserRepository.class);
        cycleService = mock(MarketCycleService.class);
        listingPopulator = mock(MarketListingPopulator.class);
        ActivityLogService activityLogService = mock(ActivityLogService.class);
        ReleaseClauseService releaseClauseService = new ReleaseClauseService(new MarketValueConfig());

        service = new MarketCycleResolutionService(
                cycleRepository, marketPlayerRepository, bidRepository,
                machineOfferRepository, leagueMemberRepository, squadRepository,
                squadPlayerRepository, userRepository, cycleService,
                listingPopulator, releaseClauseService, activityLogService
        );

        league = LeagueEntity.builder()
                .id(UUID.randomUUID()).name("L").rules(new com.fantasynations.domain.LeagueRules()).build();
        cycle = MarketCycleEntity.builder()
                .id(UUID.randomUUID()).leagueId(league.getId()).cycleNumber(1)
                .opensAt(LocalDateTime.now().minusHours(24))
                .closesAt(LocalDateTime.now())
                .status(MarketCycleStatus.OPEN).build();

        when(cycleRepository.findById(cycle.getId())).thenReturn(Optional.of(cycle));
        when(cycleRepository.save(any(MarketCycleEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(leagueMemberRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(bidRepository.save(any(BidEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(squadPlayerRepository.save(any(SquadPlayerEntity.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(cycleService.createNextCycle(any())).thenAnswer(i -> {
            var prev = (MarketCycleEntity) i.getArgument(0);
            return MarketCycleEntity.builder()
                    .id(UUID.randomUUID()).leagueId(prev.getLeagueId())
                    .cycleNumber(prev.getCycleNumber() + 1)
                    .opensAt(LocalDateTime.now()).closesAt(LocalDateTime.now().plusHours(24))
                    .status(MarketCycleStatus.OPEN).build();
        });
        when(machineOfferRepository.findByCycleIdAndStatus(any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void highestBidWinsAndMoneyIsDebited() {
        MarketPlayerEntity listing = makeListing(new BigDecimal("10000000"));
        UUID winner = userWithMoney(new BigDecimal("50000000"));
        UUID loser = userWithMoney(new BigDecimal("50000000"));

        BidEntity bigBid = bid(listing.getId(), winner, new BigDecimal("20000000"),
                LocalDateTime.now().minusMinutes(30));
        BidEntity smallBid = bid(listing.getId(), loser, new BigDecimal("15000000"),
                LocalDateTime.now().minusMinutes(60));

        when(marketPlayerRepository.findByCycleId(cycle.getId())).thenReturn(List.of(listing));
        when(bidRepository.findByMarketPlayerIdAndStatusOrderByAmountDescSubmittedAtAsc(
                eq(listing.getId()), eq(BidStatus.SUBMITTED)))
                .thenReturn(new ArrayList<>(List.of(bigBid, smallBid)));

        var result = service.resolve(cycle.getId());

        assertThat(result.transfersExecuted()).isEqualTo(1);
        assertThat(members.get(winner).getMoney()).isEqualByComparingTo(new BigDecimal("30000000"));
        assertThat(bigBid.getStatus()).isEqualTo(BidStatus.WON);
        assertThat(smallBid.getStatus()).isEqualTo(BidStatus.LOST);
        assertThat(cycle.getStatus()).isEqualTo(MarketCycleStatus.CLOSED);
    }

    @Test
    void tieBrokenByEarliestSubmittedAt() {
        MarketPlayerEntity listing = makeListing(new BigDecimal("10000000"));
        UUID earlyUser = userWithMoney(new BigDecimal("50000000"));
        UUID lateUser  = userWithMoney(new BigDecimal("50000000"));

        BidEntity earlyBid = bid(listing.getId(), earlyUser, new BigDecimal("20000000"),
                LocalDateTime.now().minusHours(2));
        BidEntity lateBid = bid(listing.getId(), lateUser, new BigDecimal("20000000"),
                LocalDateTime.now().minusMinutes(5));

        when(marketPlayerRepository.findByCycleId(cycle.getId())).thenReturn(List.of(listing));
        // Service expects highest-then-earliest order; both same amount so earlier first.
        when(bidRepository.findByMarketPlayerIdAndStatusOrderByAmountDescSubmittedAtAsc(
                eq(listing.getId()), eq(BidStatus.SUBMITTED)))
                .thenReturn(new ArrayList<>(List.of(earlyBid, lateBid)));

        service.resolve(cycle.getId());

        assertThat(earlyBid.getStatus()).isEqualTo(BidStatus.WON);
        assertThat(lateBid.getStatus()).isEqualTo(BidStatus.LOST);
    }

    @Test
    void winnerWithInsufficientFundsIsRejectedAndNextHighestWins() {
        MarketPlayerEntity listing = makeListing(new BigDecimal("10000000"));
        UUID brokeUser = userWithMoney(new BigDecimal("1000000"));
        UUID solventUser = userWithMoney(new BigDecimal("50000000"));

        BidEntity brokeBid = bid(listing.getId(), brokeUser, new BigDecimal("30000000"),
                LocalDateTime.now().minusHours(2));
        BidEntity solventBid = bid(listing.getId(), solventUser, new BigDecimal("20000000"),
                LocalDateTime.now().minusHours(1));

        when(marketPlayerRepository.findByCycleId(cycle.getId())).thenReturn(List.of(listing));
        when(bidRepository.findByMarketPlayerIdAndStatusOrderByAmountDescSubmittedAtAsc(
                eq(listing.getId()), eq(BidStatus.SUBMITTED)))
                .thenReturn(new ArrayList<>(List.of(brokeBid, solventBid)));

        var result = service.resolve(cycle.getId());

        assertThat(result.transfersExecuted()).isEqualTo(1);
        assertThat(result.rejectedNoFunds()).isEqualTo(1);
        assertThat(brokeBid.getStatus()).isEqualTo(BidStatus.REJECTED_NO_FUNDS);
        assertThat(solventBid.getStatus()).isEqualTo(BidStatus.WON);
    }

    @Test
    void listingWithNoEligibleBidsIsSimplyDropped() {
        MarketPlayerEntity listing = makeListing(new BigDecimal("10000000"));
        UUID brokeUser = userWithMoney(BigDecimal.ZERO);
        BidEntity onlyBid = bid(listing.getId(), brokeUser, new BigDecimal("5000000"),
                LocalDateTime.now().minusMinutes(10));

        when(marketPlayerRepository.findByCycleId(cycle.getId())).thenReturn(List.of(listing));
        when(bidRepository.findByMarketPlayerIdAndStatusOrderByAmountDescSubmittedAtAsc(
                eq(listing.getId()), eq(BidStatus.SUBMITTED)))
                .thenReturn(new ArrayList<>(List.of(onlyBid)));

        var result = service.resolve(cycle.getId());

        assertThat(result.transfersExecuted()).isZero();
        assertThat(result.rejectedNoFunds()).isEqualTo(1);
        assertThat(onlyBid.getStatus()).isEqualTo(BidStatus.REJECTED_NO_FUNDS);
        verify(marketPlayerRepository).delete(listing);
    }

    @Test
    void resolvingTwiceIsNoOp() {
        cycle.setStatus(MarketCycleStatus.CLOSED);
        var result = service.resolve(cycle.getId());
        assertThat(result.transfersExecuted()).isZero();
        assertThat(result.listingsResolved()).isZero();
    }

    @Test
    void resolvingOpensAndPopulatesNextCycle() {
        when(marketPlayerRepository.findByCycleId(cycle.getId())).thenReturn(List.of());

        service.resolve(cycle.getId());

        verify(cycleService, atLeastOnce()).createNextCycle(cycle);
        ArgumentCaptor<MarketCycleEntity> captor = ArgumentCaptor.forClass(MarketCycleEntity.class);
        verify(listingPopulator).populateForCycle(captor.capture());
        assertThat(captor.getValue().getCycleNumber()).isEqualTo(2);
    }

    // ------------------------------------------------------------- helpers

    private MarketPlayerEntity makeListing(BigDecimal price) {
        PlayerEntity p = PlayerEntity.builder()
                .id(UUID.randomUUID()).name("P").nationalTeam("Spain")
                .position(PlayerPosition.MID)
                .marketValue(price).initialMarketValue(price)
                .baseValue(price).currentValue(price).active(true).build();
        return MarketPlayerEntity.builder()
                .id(UUID.randomUUID()).league(league).player(p).price(price)
                .availableUntil(LocalDateTime.now()).cycleId(cycle.getId()).build();
    }

    private UUID userWithMoney(BigDecimal money) {
        UUID userId = UUID.randomUUID();
        LeagueMemberEntity m = LeagueMemberEntity.builder()
                .id(UUID.randomUUID()).league(league).money(money)
                .role(LeagueRole.MEMBER)
                .user(UserEntity.builder().id(userId).build())
                .build();
        members.put(userId, m);
        when(leagueMemberRepository.findByLeagueIdAndUserId(league.getId(), userId))
                .thenReturn(Optional.of(m));
        when(squadRepository.findByLeagueIdAndUserId(league.getId(), userId))
                .thenReturn(Optional.of(SquadEntity.builder()
                        .id(UUID.randomUUID()).league(league).user(m.getUser()).build()));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(m.getUser()));
        return userId;
    }

    private BidEntity bid(UUID listingId, UUID userId, BigDecimal amount, LocalDateTime when) {
        return BidEntity.builder()
                .id(UUID.randomUUID()).marketPlayerId(listingId).cycleId(cycle.getId())
                .userId(userId).amount(amount).status(BidStatus.SUBMITTED)
                .submittedAt(when).build();
    }
}
