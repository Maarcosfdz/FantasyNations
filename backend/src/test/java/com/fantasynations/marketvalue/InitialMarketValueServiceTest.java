package com.fantasynations.marketvalue;

import com.fantasynations.domain.Importance;
import com.fantasynations.domain.MarketValueChangeReason;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.MarketValueHistoryEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.repository.MarketValueHistoryRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialMarketValueServiceTest {

    private MarketValueConfig cfg;
    private MarketValueCalculator calculator;
    private MarketValueSeedLoader seedLoader;
    private PlayerRepository playerRepository;
    private MarketValueHistoryRepository historyRepository;
    private InitialMarketValueService service;

    @BeforeEach
    void setUp() {
        cfg = new MarketValueConfig();
        calculator = new MarketValueCalculator(cfg, new NationalTeamTierResolver(cfg));
        seedLoader = mock(MarketValueSeedLoader.class);
        when(seedLoader.findFor(any(), any())).thenReturn(java.util.Optional.empty());
        playerRepository = mock(PlayerRepository.class);
        when(playerRepository.save(any(PlayerEntity.class))).thenAnswer(inv -> {
            PlayerEntity p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        historyRepository = mock(MarketValueHistoryRepository.class);
        service = new InitialMarketValueService(calculator, cfg, seedLoader,
                playerRepository, historyRepository, new ObjectMapper());
    }

    @Test
    void appliesAlgorithmAndWritesHistory() {
        PlayerEntity p = player("Lionel Messi", "Argentina", PlayerPosition.FWD);
        service.beginImportRun();
        BigDecimal value = service.applyForNewPlayer(p);

        // FWD 10M + S 10M + STARTER fallback 12M + LOW 0 = 32M
        assertThat(value).isEqualByComparingTo(new BigDecimal("32000000"));
        assertThat(p.getMarketValue()).isEqualByComparingTo(value);
        assertThat(p.getInitialMarketValue()).isEqualByComparingTo(value);
        assertThat(p.getImportance()).isEqualTo(Importance.STARTER);

        ArgumentCaptor<MarketValueHistoryEntity> captor =
                ArgumentCaptor.forClass(MarketValueHistoryEntity.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(MarketValueChangeReason.INITIAL_VALUE);
        assertThat(captor.getValue().getOldValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captor.getValue().getNewValue()).isEqualByComparingTo(value);
        assertThat(captor.getValue().getBreakdownJson()).isNotBlank();
    }

    @Test
    void fallbackImportanceIsDeterministicByImportOrder() {
        service.beginImportRun();
        // First DEF per team -> STARTER
        PlayerEntity a = player("A", "Spain", PlayerPosition.DEF);
        PlayerEntity b = player("B", "Spain", PlayerPosition.DEF);
        PlayerEntity c = player("C", "Spain", PlayerPosition.DEF);
        PlayerEntity d = player("D", "Spain", PlayerPosition.DEF);
        PlayerEntity e = player("E", "Spain", PlayerPosition.DEF);

        service.applyForNewPlayer(a);
        service.applyForNewPlayer(b);
        service.applyForNewPlayer(c);
        service.applyForNewPlayer(d);
        service.applyForNewPlayer(e);

        assertThat(a.getImportance()).isEqualTo(Importance.STARTER);
        assertThat(b.getImportance()).isEqualTo(Importance.STARTER);
        assertThat(c.getImportance()).isEqualTo(Importance.STARTER);
        assertThat(d.getImportance()).isEqualTo(Importance.STARTER);
        // 5th DEF is rotation (next bucket of 4)
        assertThat(e.getImportance()).isEqualTo(Importance.ROTATION);
    }

    @Test
    void seedFileOverridesImportanceAndValue() {
        when(seedLoader.findFor("Star Player", "Argentina")).thenReturn(java.util.Optional.of(
                new MarketValueSeedEntry(Importance.GLOBAL_SUPERSTAR,
                        com.fantasynations.domain.LeagueReputation.ELITE,
                        new BigDecimal("70000000"),
                        null)
        ));

        PlayerEntity p = player("Star Player", "Argentina", PlayerPosition.FWD);
        service.beginImportRun();
        BigDecimal value = service.applyForNewPlayer(p);

        assertThat(value).isEqualByComparingTo(new BigDecimal("70000000"));
        assertThat(p.getImportance()).isEqualTo(Importance.GLOBAL_SUPERSTAR);
        assertThat(p.getLeagueReputation()).isEqualTo(com.fantasynations.domain.LeagueReputation.ELITE);
    }

    private PlayerEntity player(String name, String team, PlayerPosition pos) {
        return PlayerEntity.builder()
                .name(name).nationalTeam(team).position(pos)
                .baseValue(BigDecimal.ZERO).currentValue(BigDecimal.ZERO)
                .initialMarketValue(BigDecimal.ZERO).marketValue(BigDecimal.ZERO)
                .active(true)
                .build();
    }
}
