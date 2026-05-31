package com.fantasynations.marketvalue;

import com.fantasynations.domain.Importance;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReleaseClauseServiceTest {

    private ReleaseClauseService service;

    @BeforeEach
    void setUp() {
        service = new ReleaseClauseService(new MarketValueConfig());
    }

    @Test
    void effectiveClauseFollowsAutoClauseWhenNoFixed() {
        BigDecimal effective = service.effectiveClause(
                new BigDecimal("20000000"), Importance.STARTER, null);
        assertThat(effective).isEqualByComparingTo(new BigDecimal("24000000"));
    }

    @Test
    void effectiveClauseIsNeverBelowMarketValue() {
        BigDecimal effective = service.effectiveClause(
                new BigDecimal("50000000"), Importance.BENCH, new BigDecimal("10000000"));
        assertThat(effective).isGreaterThanOrEqualTo(new BigDecimal("50000000"));
    }

    @Test
    void autoClauseTracksMarketValueUp() {
        BigDecimal at20 = service.autoClause(new BigDecimal("20000000"), Importance.STARTER);
        BigDecimal at28 = service.autoClause(new BigDecimal("28000000"), Importance.STARTER);
        assertThat(at28).isGreaterThan(at20);
    }

    @Test
    void autoClauseTracksMarketValueDown() {
        BigDecimal at20 = service.autoClause(new BigDecimal("20000000"), Importance.STARTER);
        BigDecimal at15 = service.autoClause(new BigDecimal("15000000"), Importance.STARTER);
        assertThat(at15).isLessThan(at20);
    }

    @Test
    void fixedClauseDoesNotDecreaseAutomatically() {
        // user pays to raise clause to 30M
        BigDecimal afterRaise = service.effectiveClause(
                new BigDecimal("20000000"), Importance.STARTER, new BigDecimal("30000000"));
        assertThat(afterRaise).isEqualByComparingTo(new BigDecimal("30000000"));

        // market value falls to 22M -> auto would be 26.4M -> effective stays at fixed 30M
        BigDecimal later = service.effectiveClause(
                new BigDecimal("22000000"), Importance.STARTER, new BigDecimal("30000000"));
        assertThat(later).isEqualByComparingTo(new BigDecimal("30000000"));
    }

    @Test
    void autoClauseCanExceedFixedClause() {
        // fixed at 30M, market climbs to 28M -> auto is 33.6M -> effective follows auto
        BigDecimal effective = service.effectiveClause(
                new BigDecimal("28000000"), Importance.STARTER, new BigDecimal("30000000"));
        assertThat(effective).isEqualByComparingTo(new BigDecimal("33600000"));
    }

    @Test
    void cannotSetFixedClauseBelowEffective() {
        SquadPlayerEntity sp = ownership(new BigDecimal("20000000"), Importance.STARTER, null);

        // current effective = auto = 24M; requesting 23M must fail.
        assertThatThrownBy(() -> service.applyManualRaise(sp, new BigDecimal("23000000")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cannotSetFixedClauseBelowMarketValue() {
        SquadPlayerEntity sp = ownership(new BigDecimal("50000000"), Importance.STARTER, null);
        // Market value 50M; even higher than current effective auto. Requesting 40M -> below market.
        assertThatThrownBy(() -> service.applyManualRaise(sp, new BigDecimal("40000000")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void manualRaiseStoresFixedAndMarksRaisedFlag() {
        SquadPlayerEntity sp = ownership(new BigDecimal("20000000"), Importance.STARTER, null);
        BigDecimal eff = service.applyManualRaise(sp, new BigDecimal("30000000"));
        assertThat(eff).isEqualByComparingTo(new BigDecimal("30000000"));
        assertThat(sp.getFixedReleaseClauseValue()).isEqualByComparingTo(new BigDecimal("30000000"));
        assertThat(sp.isReleaseClauseManuallyRaised()).isTrue();
        assertThat(sp.getReleaseClause()).isEqualByComparingTo(new BigDecimal("30000000"));
    }

    @Test
    void missingImportanceUsesDefaultMultiplier() {
        BigDecimal auto = service.autoClause(new BigDecimal("10000000"), null);
        assertThat(auto).isEqualByComparingTo(new BigDecimal("12000000")); // 10M * 1.20
    }

    @Test
    void recalculateWritesEffectiveOnEntity() {
        SquadPlayerEntity sp = ownership(new BigDecimal("20000000"), Importance.STAR, null);
        BigDecimal eff = service.recalculate(sp);
        // STAR multiplier 1.30 -> 26M
        assertThat(eff).isEqualByComparingTo(new BigDecimal("26000000"));
        assertThat(sp.getReleaseClause()).isEqualByComparingTo(new BigDecimal("26000000"));
    }

    // helpers
    private SquadPlayerEntity ownership(BigDecimal marketValue,
                                        Importance importance,
                                        BigDecimal fixed) {
        PlayerEntity p = PlayerEntity.builder()
                .name("Test").nationalTeam("Spain").position(PlayerPosition.MID)
                .marketValue(marketValue)
                .initialMarketValue(marketValue)
                .baseValue(marketValue).currentValue(marketValue)
                .importance(importance)
                .active(true)
                .build();
        return SquadPlayerEntity.builder()
                .player(p)
                .releaseClause(BigDecimal.ZERO)
                .fixedReleaseClauseValue(fixed)
                .build();
    }
}
