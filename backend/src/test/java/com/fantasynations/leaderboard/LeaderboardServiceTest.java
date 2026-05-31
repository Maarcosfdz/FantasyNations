package com.fantasynations.leaderboard;

import com.fantasynations.domain.LeagueRole;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.LeagueMemberEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.SquadEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.RankingSnapshotRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import com.fantasynations.repository.SquadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaderboardServiceTest {

    private RankingSnapshotRepository snapshotRepository;
    private LeagueMemberRepository memberRepository;
    private SquadRepository squadRepository;
    private SquadPlayerRepository squadPlayerRepository;
    private LeaderboardService service;

    private UUID leagueId;
    private UUID viewerId;
    private UUID userA;
    private UUID userB;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(RankingSnapshotRepository.class);
        memberRepository = mock(LeagueMemberRepository.class);
        squadRepository = mock(SquadRepository.class);
        squadPlayerRepository = mock(SquadPlayerRepository.class);
        service = new LeaderboardService(snapshotRepository, memberRepository,
                squadRepository, squadPlayerRepository);

        leagueId = UUID.randomUUID();
        viewerId = UUID.randomUUID();
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();

        when(memberRepository.existsByLeagueIdAndUserId(leagueId, viewerId)).thenReturn(true);
        when(snapshotRepository.findLatestByLeagueId(leagueId)).thenReturn(List.of());
    }

    @Test
    void rankingIncludesSquadValuePerUser() {
        // Two users in the league with different squad market values.
        UserEntity ua = UserEntity.builder().id(userA).nickname("Alice").build();
        UserEntity ub = UserEntity.builder().id(userB).nickname("Bob").build();
        LeagueEntity league = LeagueEntity.builder().id(leagueId).build();
        when(memberRepository.findByLeagueId(leagueId)).thenReturn(List.of(
                LeagueMemberEntity.builder().id(UUID.randomUUID()).league(league).user(ua)
                        .role(LeagueRole.OWNER).money(BigDecimal.ZERO).build(),
                LeagueMemberEntity.builder().id(UUID.randomUUID()).league(league).user(ub)
                        .role(LeagueRole.MEMBER).money(BigDecimal.ZERO).build()
        ));

        SquadEntity squadA = SquadEntity.builder().id(UUID.randomUUID()).league(league).user(ua).build();
        SquadEntity squadB = SquadEntity.builder().id(UUID.randomUUID()).league(league).user(ub).build();
        when(squadRepository.findByLeagueId(leagueId)).thenReturn(List.of(squadA, squadB));
        when(squadPlayerRepository.findBySquadId(squadA.getId())).thenReturn(List.of(
                ownership(player("20000000")), ownership(player("15000000"))
        )); // 35M
        when(squadPlayerRepository.findBySquadId(squadB.getId())).thenReturn(List.of(
                ownership(player("80000000"))
        )); // 80M

        var ranking = service.getLeagueRanking(leagueId, viewerId);

        assertThat(ranking).hasSize(2);
        assertThat(ranking)
                .extracting(r -> r.nickname() + ":" + r.squadValue().stripTrailingZeros())
                .containsExactlyInAnyOrder(
                        "Alice:" + new BigDecimal("35000000").stripTrailingZeros(),
                        "Bob:" + new BigDecimal("80000000").stripTrailingZeros()
                );
    }

    @Test
    void squadValueIsZeroWhenUserOwnsNoPlayers() {
        UserEntity ua = UserEntity.builder().id(userA).nickname("Alice").build();
        LeagueEntity league = LeagueEntity.builder().id(leagueId).build();
        when(memberRepository.findByLeagueId(leagueId)).thenReturn(List.of(
                LeagueMemberEntity.builder().id(UUID.randomUUID()).league(league).user(ua)
                        .role(LeagueRole.OWNER).money(BigDecimal.ZERO).build()
        ));
        SquadEntity squadA = SquadEntity.builder().id(UUID.randomUUID()).league(league).user(ua).build();
        when(squadRepository.findByLeagueId(leagueId)).thenReturn(List.of(squadA));
        when(squadPlayerRepository.findBySquadId(squadA.getId())).thenReturn(List.of());

        var ranking = service.getLeagueRanking(leagueId, viewerId);

        assertThat(ranking).hasSize(1);
        assertThat(ranking.get(0).squadValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void pointsAndSquadValueAreDistinct() {
        UserEntity ua = UserEntity.builder().id(userA).nickname("Alice").build();
        LeagueEntity league = LeagueEntity.builder().id(leagueId).build();
        when(memberRepository.findByLeagueId(leagueId)).thenReturn(List.of(
                LeagueMemberEntity.builder().id(UUID.randomUUID()).league(league).user(ua)
                        .role(LeagueRole.OWNER).money(BigDecimal.ZERO).build()
        ));
        SquadEntity squadA = SquadEntity.builder().id(UUID.randomUUID()).league(league).user(ua).build();
        when(squadRepository.findByLeagueId(leagueId)).thenReturn(List.of(squadA));
        when(squadPlayerRepository.findBySquadId(squadA.getId())).thenReturn(List.of(
                ownership(player("125000000"))
        ));

        var entry = service.getLeagueRanking(leagueId, viewerId).get(0);

        // No snapshots yet -> totalPoints is 0; squadValue is 125M; clearly distinct.
        assertThat(entry.totalPoints()).isZero();
        assertThat(entry.squadValue()).isEqualByComparingTo(new BigDecimal("125000000"));
    }

    @Test
    void forbiddenForNonMembers() {
        when(memberRepository.existsByLeagueIdAndUserId(leagueId, viewerId)).thenReturn(false);
        assertThatThrownBy(() -> service.getLeagueRanking(leagueId, viewerId))
                .isInstanceOf(ForbiddenException.class);
    }

    private PlayerEntity player(String value) {
        BigDecimal v = new BigDecimal(value);
        return PlayerEntity.builder()
                .id(UUID.randomUUID()).name("P").nationalTeam("Spain")
                .position(PlayerPosition.MID)
                .baseValue(v).currentValue(v)
                .initialMarketValue(v).marketValue(v)
                .active(true).build();
    }

    private SquadPlayerEntity ownership(PlayerEntity p) {
        return SquadPlayerEntity.builder()
                .id(UUID.randomUUID()).player(p)
                .releaseClause(BigDecimal.ZERO).build();
    }
}
