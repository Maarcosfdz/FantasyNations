package com.fantasynations.market;

import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.marketvalue.MarketValueConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MachineOfferServiceTest {

    private MachineOfferService service;

    @BeforeEach
    void setUp() {
        service = new MachineOfferService(
                mock(com.fantasynations.repository.MachineOfferRepository.class),
                mock(com.fantasynations.repository.SquadPlayerRepository.class),
                mock(com.fantasynations.repository.LeagueMemberRepository.class),
                mock(com.fantasynations.repository.LeagueRepository.class),
                mock(MarketCycleService.class),
                new MarketValueConfig(),
                mock(com.fantasynations.service.ActivityLogService.class)
        );
    }

    @Test
    void offerAmountIsWithinTenPercentOfMarketValue() {
        BigDecimal marketValue = new BigDecimal("20000000");

        // Many random ids => verify bounds always hold.
        for (int i = 0; i < 200; i++) {
            SquadPlayerEntity sp = squadPlayer(marketValue);
            MarketCycleEntity cycle = MarketCycleEntity.builder()
                    .id(UUID.randomUUID()).leagueId(UUID.randomUUID())
                    .cycleNumber(1).build();
            BigDecimal amount = service.computeOfferAmount(sp, cycle);

            BigDecimal min = marketValue.multiply(new BigDecimal("0.9"));
            BigDecimal max = marketValue.multiply(new BigDecimal("1.1"));
            assertThat(amount).isBetween(min, max);
        }
    }

    @Test
    void offerIsDeterministicForSameInputs() {
        SquadPlayerEntity sp = squadPlayer(new BigDecimal("15000000"));
        MarketCycleEntity cycle = MarketCycleEntity.builder()
                .id(UUID.randomUUID()).leagueId(UUID.randomUUID())
                .cycleNumber(1).build();

        BigDecimal first  = service.computeOfferAmount(sp, cycle);
        BigDecimal second = service.computeOfferAmount(sp, cycle);
        assertThat(second).isEqualByComparingTo(first);
    }

    @Test
    void offerChangesAcrossDifferentCycles() {
        SquadPlayerEntity sp = squadPlayer(new BigDecimal("15000000"));
        MarketCycleEntity c1 = MarketCycleEntity.builder().id(UUID.randomUUID()).leagueId(UUID.randomUUID()).cycleNumber(1).build();
        MarketCycleEntity c2 = MarketCycleEntity.builder().id(UUID.randomUUID()).leagueId(UUID.randomUUID()).cycleNumber(2).build();
        BigDecimal a = service.computeOfferAmount(sp, c1);
        BigDecimal b = service.computeOfferAmount(sp, c2);
        assertThat(a).isNotEqualByComparingTo(b);
    }

    private SquadPlayerEntity squadPlayer(BigDecimal marketValue) {
        PlayerEntity p = PlayerEntity.builder()
                .id(UUID.randomUUID()).name("P").nationalTeam("Spain")
                .position(com.fantasynations.domain.PlayerPosition.MID)
                .marketValue(marketValue).initialMarketValue(marketValue)
                .baseValue(marketValue).currentValue(marketValue).active(true).build();
        return SquadPlayerEntity.builder()
                .id(UUID.randomUUID()).player(p).releaseClause(BigDecimal.ZERO).build();
    }
}
