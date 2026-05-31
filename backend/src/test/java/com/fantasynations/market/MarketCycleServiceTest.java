package com.fantasynations.market;

import com.fantasynations.domain.LeagueRules;
import com.fantasynations.domain.MarketCycleStatus;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.MarketCycleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketCycleServiceTest {

    private MarketCycleRepository cycleRepository;
    private LeagueRepository leagueRepository;
    private MarketCycleService service;

    private UUID leagueId;
    private LeagueEntity league;

    @BeforeEach
    void setUp() {
        cycleRepository = mock(MarketCycleRepository.class);
        leagueRepository = mock(LeagueRepository.class);
        service = new MarketCycleService(cycleRepository, leagueRepository);

        leagueId = UUID.randomUUID();
        LeagueRules rules = new LeagueRules();
        rules.setMarketRefreshIntervalHours(24);
        league = LeagueEntity.builder().id(leagueId).name("L").rules(rules).build();

        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(cycleRepository.save(any(MarketCycleEntity.class))).thenAnswer(inv -> {
            MarketCycleEntity c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
    }

    @Test
    void createsFirstCycleWhenNoneExists() {
        when(cycleRepository.findFirstByLeagueIdAndStatusOrderByCycleNumberDesc(
                leagueId, MarketCycleStatus.OPEN)).thenReturn(Optional.empty());
        when(cycleRepository.findFirstByLeagueIdOrderByCycleNumberDesc(leagueId))
                .thenReturn(Optional.empty());

        MarketCycleEntity created = service.getOrCreateOpenCycle(leagueId);

        assertThat(created.getCycleNumber()).isEqualTo(1);
        assertThat(created.getStatus()).isEqualTo(MarketCycleStatus.OPEN);
        assertThat(created.getClosesAt())
                .isAfterOrEqualTo(created.getOpensAt().plusHours(23).plusMinutes(59));
    }

    @Test
    void getOrCreateReturnsExistingOpenCycle() {
        MarketCycleEntity existing = MarketCycleEntity.builder()
                .id(UUID.randomUUID()).leagueId(leagueId).cycleNumber(3)
                .status(MarketCycleStatus.OPEN)
                .opensAt(LocalDateTime.now())
                .closesAt(LocalDateTime.now().plusHours(2)).build();
        when(cycleRepository.findFirstByLeagueIdAndStatusOrderByCycleNumberDesc(
                leagueId, MarketCycleStatus.OPEN)).thenReturn(Optional.of(existing));

        assertThat(service.getOrCreateOpenCycle(leagueId)).isSameAs(existing);
    }

    @Test
    void createsNextCycleAfterAClosedOne() {
        MarketCycleEntity previous = MarketCycleEntity.builder()
                .id(UUID.randomUUID()).leagueId(leagueId).cycleNumber(2)
                .status(MarketCycleStatus.CLOSED)
                .opensAt(LocalDateTime.now().minusHours(48))
                .closesAt(LocalDateTime.now().minusHours(24))
                .resolvedAt(LocalDateTime.now().minusHours(24)).build();

        MarketCycleEntity next = service.createNextCycle(previous);

        assertThat(next.getCycleNumber()).isEqualTo(3);
        assertThat(next.getStatus()).isEqualTo(MarketCycleStatus.OPEN);
        // closes 24 h after the previous resolution
        assertThat(next.getClosesAt()).isEqualTo(previous.getResolvedAt().plusHours(24));
    }

    @Test
    void cycleDurationUsesLeagueRules() {
        LeagueRules rules = new LeagueRules();
        rules.setMarketRefreshIntervalHours(6); // shorter cycle
        league.setRules(rules);
        when(cycleRepository.findFirstByLeagueIdAndStatusOrderByCycleNumberDesc(
                leagueId, MarketCycleStatus.OPEN)).thenReturn(Optional.empty());
        when(cycleRepository.findFirstByLeagueIdOrderByCycleNumberDesc(leagueId))
                .thenReturn(Optional.empty());

        MarketCycleEntity created = service.getOrCreateOpenCycle(leagueId);
        long hours = java.time.Duration.between(created.getOpensAt(), created.getClosesAt()).toHours();
        assertThat(hours).isEqualTo(6);
    }
}
