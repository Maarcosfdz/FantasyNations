package com.fantasynations.scoring;

import com.fantasynations.domain.LeagueRole;
import com.fantasynations.domain.LeagueRules;
import com.fantasynations.domain.MatchdayAggregationReason;
import com.fantasynations.domain.MatchdayPhase;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.domain.RealMatchStatus;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.LeagueMemberEntity;
import com.fantasynations.entity.LineupEntity;
import com.fantasynations.entity.LineupPlayerEntity;
import com.fantasynations.entity.LockedLineupPlayerEntity;
import com.fantasynations.entity.MatchdayEntity;
import com.fantasynations.entity.MatchdayScoreEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.PlayerMatchStatsEntity;
import com.fantasynations.entity.PlayerMatchdayScoreEntity;
import com.fantasynations.entity.RealMatchEntity;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.LineupRepository;
import com.fantasynations.repository.LockedLineupPlayerRepository;
import com.fantasynations.repository.MatchdayRepository;
import com.fantasynations.repository.MatchdayScoreRepository;
import com.fantasynations.repository.PlayerMatchStatsRepository;
import com.fantasynations.repository.PlayerMatchdayScoreRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fantasynations.repository.RealMatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchdayAggregationServiceTest {

    private LockedLineupPlayerRepository lockedRepo;
    private LineupRepository lineupRepo;
    private LeagueMemberRepository memberRepo;
    private LeagueRepository leagueRepo;
    private MatchdayRepository matchdayRepo;
    private RealMatchRepository realMatchRepo;
    private MatchdayScoreRepository scoreRepo;
    private PlayerMatchdayScoreRepository playerScoreRepo;
    private PlayerMatchStatsRepository statsRepo;
    private PlayerRepository playerRepo;
    private MatchdayAggregationService service;

    private UUID leagueId;
    private UUID userId;
    private UUID matchdayId;
    private UUID realMatchId;
    private LeagueEntity league;
    private MatchdayEntity matchday;
    private LeagueMemberEntity member;
    private LineupEntity lineup;
    private final List<LockedLineupPlayerEntity> snapshotStore = new ArrayList<>();
    private final List<MatchdayScoreEntity> scoreStore = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lockedRepo = mock(LockedLineupPlayerRepository.class);
        lineupRepo = mock(LineupRepository.class);
        memberRepo = mock(LeagueMemberRepository.class);
        leagueRepo = mock(LeagueRepository.class);
        matchdayRepo = mock(MatchdayRepository.class);
        realMatchRepo = mock(RealMatchRepository.class);
        scoreRepo = mock(MatchdayScoreRepository.class);
        playerScoreRepo = mock(PlayerMatchdayScoreRepository.class);
        statsRepo = mock(PlayerMatchStatsRepository.class);
        playerRepo = mock(PlayerRepository.class);

        var scoringService = new FantasyScoringService(TestScoringRules.provider());
        var mapper = new PlayerMatchStatsMapper();
        var freezeService = new LineupFreezeService(lockedRepo, lineupRepo);
        service = new MatchdayAggregationService(
                freezeService, scoringService, mapper,
                matchdayRepo, realMatchRepo, lineupRepo, leagueRepo,
                memberRepo, scoreRepo, playerScoreRepo, statsRepo, playerRepo,
                new ObjectMapper());

        leagueId = UUID.randomUUID();
        userId = UUID.randomUUID();
        matchdayId = UUID.randomUUID();
        realMatchId = UUID.randomUUID();

        league = LeagueEntity.builder()
                .id(leagueId).name("L").rules(new LeagueRules()).build();
        matchday = MatchdayEntity.builder()
                .id(matchdayId).number(1).phase(MatchdayPhase.GROUP).build();
        UserEntity user = UserEntity.builder().id(userId).build();
        member = LeagueMemberEntity.builder()
                .id(UUID.randomUUID()).league(league).user(user)
                .role(LeagueRole.MEMBER)
                .money(new BigDecimal("100000000")).build();

        when(matchdayRepo.findById(matchdayId)).thenReturn(Optional.of(matchday));
        when(leagueRepo.findById(leagueId)).thenReturn(Optional.of(league));
        when(memberRepo.findByLeagueIdAndUserId(leagueId, userId)).thenReturn(Optional.of(member));
        when(realMatchRepo.findByMatchdayId(matchdayId))
                .thenReturn(List.of(RealMatchEntity.builder()
                        .id(realMatchId).matchdayId(matchdayId)
                        .kickoff(LocalDateTime.now().minusHours(2))
                        .homeTeam("A").awayTeam("B")
                        .status(RealMatchStatus.FINISHED).build()));

        when(lockedRepo.findByLineupIdAndMatchdayId(any(), eq(matchdayId)))
                .thenAnswer(inv -> new ArrayList<>(snapshotStore));
        when(lockedRepo.saveAll(any())).thenAnswer(inv -> {
            Iterable<LockedLineupPlayerEntity> in = inv.getArgument(0);
            in.forEach(e -> {
                if (e.getId() == null) e.setId(UUID.randomUUID());
                snapshotStore.add(e);
            });
            return snapshotStore;
        });
        when(lineupRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(scoreRepo.findByLeagueIdAndUserIdAndMatchdayId(eq(leagueId), eq(userId), eq(matchdayId)))
                .thenAnswer(inv -> scoreStore.stream().findFirst());
        when(scoreRepo.save(any())).thenAnswer(inv -> {
            MatchdayScoreEntity s = inv.getArgument(0);
            if (s.getId() == null) s.setId(UUID.randomUUID());
            scoreStore.removeIf(e -> e.getId().equals(s.getId()));
            scoreStore.add(s);
            return s;
        });
        when(playerScoreRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(playerRepo.findById(any())).thenAnswer(inv ->
                Optional.of(player((UUID) inv.getArgument(0), PlayerPosition.MID, "P")));
    }

    @Test
    void incompleteLineupScoresZeroWithReason() {
        lineup = lineupWith(List.of()); // 0 players
        when(lineupRepo.findByLeagueIdAndUserId(leagueId, userId)).thenReturn(Optional.of(lineup));

        var result = service.aggregate(leagueId, userId, matchdayId);

        assertThat(result.totalPoints()).isZero();
        assertThat(result.reason()).isEqualTo(MatchdayAggregationReason.INCOMPLETE_LINEUP);
    }

    @Test
    void negativeBalanceScoresZeroWithReason() {
        lineup = lineupWith(eleven());
        when(lineupRepo.findByLeagueIdAndUserId(leagueId, userId)).thenReturn(Optional.of(lineup));
        member.setMoney(new BigDecimal("-1"));

        var result = service.aggregate(leagueId, userId, matchdayId);

        assertThat(result.totalPoints()).isZero();
        assertThat(result.reason()).isEqualTo(MatchdayAggregationReason.NEGATIVE_BALANCE);
    }

    @Test
    void happyPathSumsFrozenLineupScores() {
        List<LineupPlayerEntity> players = eleven();
        lineup = lineupWith(players);
        when(lineupRepo.findByLeagueIdAndUserId(leagueId, userId)).thenReturn(Optional.of(lineup));

        // Give the first player a goal in the FINISHED match.
        UUID scoringPlayerId = players.get(0).getPlayer().getId();
        when(statsRepo.findByRealMatchId(realMatchId)).thenReturn(List.of(
                stats(scoringPlayerId, realMatchId, b -> { b.goals = 1; b.minutesPlayed = 90; })
        ));
        when(playerRepo.findById(scoringPlayerId))
                .thenReturn(Optional.of(player(scoringPlayerId, PlayerPosition.FWD, "Scorer")));

        var result = service.aggregate(leagueId, userId, matchdayId);

        // Scorer: 2 (minutes) + 3 (FWD goal) = 5; other 10 players score 0 (no stats).
        assertThat(result.reason()).isEqualTo(MatchdayAggregationReason.OK);
        assertThat(result.totalPoints()).isEqualTo(5);
    }

    @Test
    void reaggregatingReusesSnapshotAndDoesNotDoubleFreeze() {
        lineup = lineupWith(eleven());
        when(lineupRepo.findByLeagueIdAndUserId(leagueId, userId)).thenReturn(Optional.of(lineup));

        service.aggregate(leagueId, userId, matchdayId);
        service.aggregate(leagueId, userId, matchdayId);

        // saveAll(snapshot) called only on first run.
        verify(lockedRepo, times(1)).saveAll(any());
    }

    @Test
    void postLockLineupEditsDoNotAffectMatchdayScore() {
        List<LineupPlayerEntity> players = eleven();
        lineup = lineupWith(players);
        when(lineupRepo.findByLeagueIdAndUserId(leagueId, userId)).thenReturn(Optional.of(lineup));

        // First aggregation freezes the original 11.
        service.aggregate(leagueId, userId, matchdayId);
        int snapshotSize = snapshotStore.size();

        // User edits the live lineup AFTER lock: remove 5 players.
        lineup.getPlayers().subList(0, 5).clear();

        // Second aggregation still sees the original snapshot of 11.
        var result = service.aggregate(leagueId, userId, matchdayId);
        assertThat(snapshotStore).hasSize(snapshotSize); // unchanged
        assertThat(result.reason()).isEqualTo(MatchdayAggregationReason.OK);
    }

    // --- helpers --------------------------------------------------------

    private LineupEntity lineupWith(List<LineupPlayerEntity> players) {
        return LineupEntity.builder()
                .id(UUID.randomUUID())
                .league(league)
                .user(member.getUser())
                .players(new ArrayList<>(players))
                .build();
    }

    private List<LineupPlayerEntity> eleven() {
        return java.util.stream.IntStream.range(0, 11).mapToObj(i -> {
            PlayerEntity p = player(UUID.randomUUID(),
                    i == 0 ? PlayerPosition.GK : PlayerPosition.MID, "Pl-" + i);
            return LineupPlayerEntity.builder()
                    .id(UUID.randomUUID()).player(p).positionSlot("SLOT-" + i).build();
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private static PlayerEntity player(UUID id, PlayerPosition pos, String name) {
        return PlayerEntity.builder()
                .id(id).name(name).nationalTeam("T").position(pos)
                .baseValue(BigDecimal.ONE).currentValue(BigDecimal.ONE)
                .initialMarketValue(BigDecimal.ONE).marketValue(BigDecimal.ONE)
                .active(true).build();
    }

    @FunctionalInterface
    private interface StatsCustomizer { void customize(StatsBuilder b); }

    private PlayerMatchStatsEntity stats(UUID playerId, UUID matchId, StatsCustomizer c) {
        StatsBuilder b = new StatsBuilder();
        c.customize(b);
        return PlayerMatchStatsEntity.builder()
                .id(UUID.randomUUID()).playerId(playerId).realMatchId(matchId)
                .minutesPlayed(b.minutesPlayed).goals(b.goals)
                .build();
    }

    private static final class StatsBuilder {
        int minutesPlayed, goals;
    }
}
