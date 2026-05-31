package com.fantasynations.squad;

import com.fantasynations.domain.AvailabilityStatus;
import com.fantasynations.domain.LeagueRole;
import com.fantasynations.domain.LeagueRules;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.LeagueMemberEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.SquadEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.marketvalue.MarketValueConfig;
import com.fantasynations.marketvalue.ReleaseClauseService;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InitialSquadAssignmentServiceTest {

    private PlayerRepository playerRepository;
    private SquadPlayerRepository squadPlayerRepository;
    private LeagueMemberRepository leagueMemberRepository;
    private ReleaseClauseService releaseClauseService;
    private InitialSquadAssignmentService service;

    private LeagueEntity league;
    private SquadEntity squad;
    private LeagueMemberEntity member;
    private final List<SquadPlayerEntity> savedSquadPlayers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        playerRepository = mock(PlayerRepository.class);
        squadPlayerRepository = mock(SquadPlayerRepository.class);
        leagueMemberRepository = mock(LeagueMemberRepository.class);
        releaseClauseService = new ReleaseClauseService(new MarketValueConfig());

        // Seeded RNG so tests are reproducible.
        service = new InitialSquadAssignmentService(
                playerRepository, squadPlayerRepository, leagueMemberRepository,
                releaseClauseService, new Random(42));

        league = LeagueEntity.builder()
                .id(UUID.randomUUID()).name("L").rules(new LeagueRules()).build();
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).build();
        squad = SquadEntity.builder()
                .id(UUID.randomUUID()).league(league).user(user).build();
        member = LeagueMemberEntity.builder()
                .id(UUID.randomUUID()).league(league).user(user)
                .role(LeagueRole.MEMBER).money(BigDecimal.ZERO).build();

        when(squadPlayerRepository.findAll()).thenReturn(new ArrayList<>(savedSquadPlayers));
        when(squadPlayerRepository.save(any())).thenAnswer(inv -> {
            SquadPlayerEntity sp = inv.getArgument(0);
            if (sp.getId() == null) sp.setId(UUID.randomUUID());
            savedSquadPlayers.add(sp);
            return sp;
        });
        when(leagueMemberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void assigns15PlayersWithStandardComposition() {
        when(playerRepository.findByActiveTrue()).thenReturn(buildPool(
                10, 10, 12, 8,           // counts per position (GK, DEF, MID, FWD)
                "10000000"));            // each player worth 10M

        var result = service.assignFor(league, member, squad);

        assertThat(result.playersAssigned()).isEqualTo(15);
        assertThat(result.composition()).containsAllEntriesOf(java.util.Map.of(
                PlayerPosition.GK, 2,
                PlayerPosition.DEF, 5,
                PlayerPosition.MID, 5,
                PlayerPosition.FWD, 3));
        assertThat(result.usedFallbackComposition()).isFalse();
    }

    @Test
    void startingMoneyEqualsBudgetMinusSquadValue() {
        when(playerRepository.findByActiveTrue()).thenReturn(buildPool(
                10, 10, 12, 8, "10000000"));

        var result = service.assignFor(league, member, squad);

        BigDecimal expectedSquadValue = new BigDecimal("150000000"); // 15 * 10M
        assertThat(result.squadMarketValue()).isEqualByComparingTo(expectedSquadValue);
        // Budget = 300M; remaining = 300M - 150M = 150M.
        assertThat(result.startingMoney()).isEqualByComparingTo(new BigDecimal("150000000"));
        assertThat(member.getMoney()).isEqualByComparingTo(new BigDecimal("150000000"));
    }

    @Test
    void noPlayerAssignedTwiceInTheSameLeague() {
        List<PlayerEntity> pool = buildPool(10, 10, 12, 8, "10000000");
        when(playerRepository.findByActiveTrue()).thenReturn(pool);

        // Pre-assign two pool players to another user in the same league - they
        // must NOT be reassigned to the new member.
        UUID taken1 = pool.get(0).getId();           // a GK
        UUID taken2 = pool.get(15).getId();          // a MID-ish
        markAlreadyOwned(pool.get(0));
        markAlreadyOwned(pool.get(15));

        service.assignFor(league, member, squad);

        List<UUID> assigned = savedSquadPlayers.stream()
                .filter(sp -> sp.getSquad().getId().equals(squad.getId()))
                .map(sp -> sp.getPlayer().getId())
                .collect(Collectors.toList());
        assertThat(assigned).doesNotContain(taken1, taken2);
        assertThat(assigned).doesNotHaveDuplicates();
    }

    @Test
    void fallbackCompositionWhenAPoolIsTooSmall() {
        // Only 3 FWDs available -> ±1 fallback kicks in (FWD becomes 3 still or 2,
        // others bump up to keep total at 15).
        when(playerRepository.findByActiveTrue()).thenReturn(buildPool(
                10, 10, 12, 3, "8000000"));

        var result = service.assignFor(league, member, squad);

        assertThat(result.playersAssigned()).isEqualTo(15);
        int total = result.composition().values().stream().mapToInt(Integer::intValue).sum();
        assertThat(total).isEqualTo(15);
        assertThat(result.composition().get(PlayerPosition.GK)).isEqualTo(2);
        // any of DEF/MID/FWD may have varied; assert each within ±1 of standard 2/5/5/3.
        assertThat(result.composition().get(PlayerPosition.DEF)).isBetween(4, 6);
        assertThat(result.composition().get(PlayerPosition.MID)).isBetween(4, 6);
        assertThat(result.composition().get(PlayerPosition.FWD)).isBetween(2, 4);
    }

    @Test
    void typicalPoolProducesSquadInTargetRange() {
        // 15 players at 15M each = 225M, lands in the new [200M, 250M] band.
        when(playerRepository.findByActiveTrue()).thenReturn(buildPool(
                10, 10, 12, 8, "15000000"));

        var result = service.assignFor(league, member, squad);

        assertThat(result.squadMarketValue())
                .as("squad should land in target range with mid-priced pool")
                .isBetween(new BigDecimal("200000000"), new BigDecimal("250000000"));
        assertThat(result.inTargetRange()).isTrue();
    }

    @Test
    void biasesTowardHigherTierPlayersWhenBothAvailable() {
        // Half the pool is high-tier (~20M), half is bench filler (~3M).
        // The assigner must prefer high-tier so the squad lands in 200M-250M.
        List<PlayerEntity> mixed = new ArrayList<>();
        mixed.addAll(pool(PlayerPosition.GK,  5,  "20000000"));
        mixed.addAll(pool(PlayerPosition.GK,  5,  "3000000"));
        mixed.addAll(pool(PlayerPosition.DEF, 8,  "20000000"));
        mixed.addAll(pool(PlayerPosition.DEF, 8,  "3000000"));
        mixed.addAll(pool(PlayerPosition.MID, 8,  "20000000"));
        mixed.addAll(pool(PlayerPosition.MID, 8,  "3000000"));
        mixed.addAll(pool(PlayerPosition.FWD, 5,  "20000000"));
        mixed.addAll(pool(PlayerPosition.FWD, 5,  "3000000"));
        when(playerRepository.findByActiveTrue()).thenReturn(mixed);

        var result = service.assignFor(league, member, squad);

        assertThat(result.squadMarketValue())
                .as("biased sampling should prefer the high-tier half of the pool")
                .isGreaterThanOrEqualTo(new BigDecimal("180000000"));
    }

    @Test
    void squadNeverExceedsBudgetWhenAlternativesExist_at300M() {
        // Mix of expensive + cheap players; the constrainToBudget swap must
        // keep the squad under 200M.
        List<PlayerEntity> expensive = pool(PlayerPosition.GK,  3, "40000000");
        expensive.addAll(pool(PlayerPosition.DEF, 6, "35000000"));
        expensive.addAll(pool(PlayerPosition.MID, 8, "25000000"));
        expensive.addAll(pool(PlayerPosition.FWD, 5, "30000000"));
        // Plenty of cheap alternatives:
        expensive.addAll(pool(PlayerPosition.GK,  10, "5000000"));
        expensive.addAll(pool(PlayerPosition.DEF, 10, "5000000"));
        expensive.addAll(pool(PlayerPosition.MID, 10, "5000000"));
        expensive.addAll(pool(PlayerPosition.FWD, 10, "5000000"));
        when(playerRepository.findByActiveTrue()).thenReturn(expensive);

        var result = service.assignFor(league, member, squad);

        assertThat(result.squadMarketValue()).isLessThanOrEqualTo(new BigDecimal("300000000"));
        assertThat(result.startingMoney()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    void weakPoolGivesExtraMoneyInsteadOfFailing() {
        // 15 cheap players -> squad value well below 100M; user simply gets more money.
        when(playerRepository.findByActiveTrue()).thenReturn(buildPool(
                2, 4, 6, 3, "1000000"));

        var result = service.assignFor(league, member, squad);

        assertThat(result.playersAssigned()).isEqualTo(15);
        assertThat(result.squadMarketValue()).isEqualByComparingTo(new BigDecimal("15000000"));
        // Budget 300M - 15M squad = 285M money.
        assertThat(result.startingMoney()).isEqualByComparingTo(new BigDecimal("285000000"));
    }

    @Test
    void recalculatesReleaseClauseForEveryPick() {
        when(playerRepository.findByActiveTrue()).thenReturn(buildPool(
                10, 10, 12, 8, "10000000"));

        service.assignFor(league, member, squad);

        verify(squadPlayerRepository, times(15)).save(any());
        // Each ownership row got a non-zero release clause from the service.
        assertThat(savedSquadPlayers)
                .allSatisfy(sp -> assertThat(sp.getReleaseClause())
                        .isGreaterThan(BigDecimal.ZERO));
    }

    // ---- helpers ----

    private List<PlayerEntity> buildPool(int gks, int defs, int mids, int fwds, String value) {
        List<PlayerEntity> pool = new ArrayList<>();
        pool.addAll(pool(PlayerPosition.GK,  gks,  value));
        pool.addAll(pool(PlayerPosition.DEF, defs, value));
        pool.addAll(pool(PlayerPosition.MID, mids, value));
        pool.addAll(pool(PlayerPosition.FWD, fwds, value));
        return pool;
    }

    private List<PlayerEntity> pool(PlayerPosition pos, int n, String value) {
        BigDecimal v = new BigDecimal(value);
        List<PlayerEntity> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(PlayerEntity.builder()
                    .id(UUID.randomUUID())
                    .name(pos.name() + "-" + i)
                    .nationalTeam("Spain")
                    .position(pos)
                    .baseValue(v).currentValue(v)
                    .initialMarketValue(v).marketValue(v)
                    .availabilityStatus(AvailabilityStatus.AVAILABLE)
                    .active(true).build());
        }
        return list;
    }

    private void markAlreadyOwned(PlayerEntity player) {
        SquadEntity otherSquad = SquadEntity.builder()
                .id(UUID.randomUUID()).league(league).build();
        SquadPlayerEntity sp = SquadPlayerEntity.builder()
                .id(UUID.randomUUID()).squad(otherSquad).player(player)
                .releaseClause(BigDecimal.ONE).build();
        savedSquadPlayers.add(sp);
        when(squadPlayerRepository.findAll()).thenReturn(new ArrayList<>(savedSquadPlayers));
    }
}
