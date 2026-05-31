package com.fantasynations.marketvalue;

import com.fantasynations.domain.AvailabilityStatus;
import com.fantasynations.domain.Importance;
import com.fantasynations.domain.MarketValueChangeReason;
import com.fantasynations.domain.MatchdayPhase;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.domain.RealMatchStatus;
import com.fantasynations.entity.MarketValueHistoryEntity;
import com.fantasynations.entity.MatchdayEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.PlayerMatchStatsEntity;
import com.fantasynations.entity.RealMatchEntity;
import com.fantasynations.entity.SquadEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.repository.MarketValueHistoryRepository;
import com.fantasynations.repository.MatchdayRepository;
import com.fantasynations.repository.PlayerMatchStatsRepository;
import com.fantasynations.repository.PlayerMatchdayScoreRepository;
import com.fantasynations.repository.PlayerMatchdayScoreRepository.PlayerAvgRow;
import com.fantasynations.repository.PlayerMatchdayScoreRepository.PlayerPointsRow;
import com.fantasynations.repository.PlayerRepository;
import com.fantasynations.repository.RealMatchRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicMarketValueServiceTest {

    private MarketValueHistoryRepository historyRepository;
    private PlayerRepository playerRepository;
    private MatchdayRepository matchdayRepository;
    private RealMatchRepository realMatchRepository;
    private PlayerMatchStatsRepository statsRepository;
    private PlayerMatchdayScoreRepository playerScoreRepository;
    private SquadPlayerRepository squadPlayerRepository;
    private ReleaseClauseService releaseClauseService;
    private DynamicMarketValueService service;

    private MatchdayEntity matchday;
    private RealMatchEntity realMatch;
    private final Map<UUID, PlayerEntity> players = new HashMap<>();
    private final List<MarketValueHistoryEntity> history = new ArrayList<>();

    @BeforeEach
    void setUp() {
        historyRepository = mock(MarketValueHistoryRepository.class);
        playerRepository = mock(PlayerRepository.class);
        matchdayRepository = mock(MatchdayRepository.class);
        realMatchRepository = mock(RealMatchRepository.class);
        statsRepository = mock(PlayerMatchStatsRepository.class);
        playerScoreRepository = mock(PlayerMatchdayScoreRepository.class);
        squadPlayerRepository = mock(SquadPlayerRepository.class);

        MarketValueConfig cfg = new MarketValueConfig();
        var calculator = new MarketValueCalculator(cfg, new NationalTeamTierResolver(cfg));
        releaseClauseService = new ReleaseClauseService(cfg);

        service = new DynamicMarketValueService(
                calculator, historyRepository, playerRepository, matchdayRepository,
                realMatchRepository, statsRepository, playerScoreRepository,
                squadPlayerRepository, releaseClauseService, new ObjectMapper());

        matchday = MatchdayEntity.builder()
                .id(UUID.randomUUID()).number(3).phase(MatchdayPhase.GROUP).build();
        realMatch = RealMatchEntity.builder()
                .id(UUID.randomUUID()).matchdayId(matchday.getId())
                .kickoff(LocalDateTime.now().minusDays(1))
                .homeTeam("A").awayTeam("B").status(RealMatchStatus.FINISHED).build();

        when(matchdayRepository.findById(matchday.getId())).thenReturn(Optional.of(matchday));
        when(realMatchRepository.findByMatchdayId(matchday.getId())).thenReturn(List.of(realMatch));
        when(playerRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(players.get(inv.getArgument(0))));
        when(historyRepository.existsByPlayerIdAndMatchdayId(any(), any())).thenReturn(false);
        when(historyRepository.save(any())).thenAnswer(inv -> {
            MarketValueHistoryEntity h = inv.getArgument(0);
            if (h.getId() == null) h.setId(UUID.randomUUID());
            history.add(h);
            return h;
        });
        when(playerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(squadPlayerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(playerScoreRepository.findHistoricalAveragesBefore(anyInt())).thenReturn(List.of());
    }

    @Test
    void highScoreIncreasesMarketValueAndWritesHistory() {
        PlayerEntity p = newPlayer(PlayerPosition.FWD, "20000000");
        givePlayerStats(p.getId(), 90, false);
        givePersistedPoints(p.getId(), 12); // high

        var result = service.applyForMatchday(matchday.getId());

        assertThat(result.playersUpdated()).isEqualTo(1);
        assertThat(p.getMarketValue()).isGreaterThan(new BigDecimal("20000000"));
        assertThat(p.getInitialMarketValue()).isEqualByComparingTo("20000000"); // never touched
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getReason()).isEqualTo(MarketValueChangeReason.HIGH_PERFORMANCE);
        assertThat(history.get(0).getMatchdayId()).isEqualTo(matchday.getId());
    }

    @Test
    void badPerformanceDecreasesMarketValue() {
        // A player with negative points who didn't make the 60-minute base:
        // pointsScore = -4 (negative), minutesScore = 0 (under 60), availability 0,
        // tournament 0 -> momentum -4 -> -7%.
        PlayerEntity p = newPlayer(PlayerPosition.MID, "20000000");
        givePlayerStats(p.getId(), 30, false);
        givePersistedPoints(p.getId(), -3);

        service.applyForMatchday(matchday.getId());

        assertThat(p.getMarketValue()).isLessThan(new BigDecimal("20000000"));
        assertThat(history.get(0).getReason()).isEqualTo(MarketValueChangeReason.LOW_PERFORMANCE);
    }

    @Test
    void injuredPlayerHitsAtLeastEightPercentFloor() {
        PlayerEntity p = newPlayer(PlayerPosition.FWD, "30000000");
        p.setAvailabilityStatus(AvailabilityStatus.INJURED);
        givePlayerStats(p.getId(), 90, false);
        givePersistedPoints(p.getId(), 15); // big positive that would otherwise lift value

        service.applyForMatchday(matchday.getId());

        // injured floor = -8% => 30M -> at most 27.6M
        assertThat(p.getMarketValue()).isLessThanOrEqualTo(new BigDecimal("27600000"));
        assertThat(history.get(0).getReason()).isEqualTo(MarketValueChangeReason.INJURED);
    }

    @Test
    void suspendedPlayerHitsAtLeastFivePercentFloor() {
        PlayerEntity p = newPlayer(PlayerPosition.DEF, "30000000");
        p.setAvailabilityStatus(AvailabilityStatus.SUSPENDED);
        givePlayerStats(p.getId(), 0, true);
        givePersistedPoints(p.getId(), 0);

        service.applyForMatchday(matchday.getId());

        // suspended floor = -5% => at most 28.5M
        assertThat(p.getMarketValue()).isLessThanOrEqualTo(new BigDecimal("28500000"));
        assertThat(history.get(0).getReason()).isEqualTo(MarketValueChangeReason.SUSPENDED);
    }

    @Test
    void historyEntryAlwaysCreatedEvenIfDeltaIsZero() {
        PlayerEntity p = newPlayer(PlayerPosition.MID, "20000000");
        givePlayerStats(p.getId(), 70, false);
        givePersistedPoints(p.getId(), 1); // small positive -> ~+2% but may or may not change after rounding

        service.applyForMatchday(matchday.getId());

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getMatchdayId()).isEqualTo(matchday.getId());
        assertThat(history.get(0).getPlayerId()).isEqualTo(p.getId());
    }

    @Test
    void repeatedApplyForSameMatchdayIsIdempotent() {
        PlayerEntity p = newPlayer(PlayerPosition.FWD, "20000000");
        givePlayerStats(p.getId(), 90, false);
        givePersistedPoints(p.getId(), 12);

        service.applyForMatchday(matchday.getId());
        // Simulate that a history row exists for this (player, matchday).
        when(historyRepository.existsByPlayerIdAndMatchdayId(eq(p.getId()), eq(matchday.getId())))
                .thenReturn(true);
        history.clear();

        var second = service.applyForMatchday(matchday.getId());

        assertThat(second.playersUpdated()).isZero();
        assertThat(second.playersSkippedIdempotent()).isEqualTo(1);
        assertThat(history).isEmpty();
    }

    @Test
    void releaseClausesRecomputedAfterMarketValueChange() {
        PlayerEntity p = newPlayer(PlayerPosition.FWD, "20000000");
        p.setImportance(Importance.STARTER);
        givePlayerStats(p.getId(), 90, false);
        givePersistedPoints(p.getId(), 12);

        SquadPlayerEntity sp = SquadPlayerEntity.builder()
                .id(UUID.randomUUID())
                .squad(SquadEntity.builder().id(UUID.randomUUID()).build())
                .player(p)
                .releaseClause(new BigDecimal("24000000")) // = 20M * 1.20 STARTER
                .build();
        when(squadPlayerRepository.findByPlayerId(p.getId())).thenReturn(List.of(sp));

        service.applyForMatchday(matchday.getId());

        // After increase, releaseClause must follow the new marketValue (no fixed clause).
        BigDecimal expectedAuto = p.getMarketValue().multiply(new BigDecimal("1.20"))
                .setScale(0, java.math.RoundingMode.HALF_UP);
        assertThat(sp.getReleaseClause()).isEqualByComparingTo(expectedAuto);
        verify(squadPlayerRepository, times(1)).save(sp);
    }

    @Test
    void noStatsMeansPlayerNotProcessed() {
        PlayerEntity p = newPlayer(PlayerPosition.MID, "20000000");
        // No stats for this player -> not in the universe.
        when(statsRepository.findByRealMatchIdIn(any())).thenReturn(List.of());
        givePersistedPoints(p.getId(), 8);

        var result = service.applyForMatchday(matchday.getId());

        assertThat(result.playersConsidered()).isZero();
        assertThat(history).isEmpty();
    }

    // ---- helpers ----

    private PlayerEntity newPlayer(PlayerPosition pos, String value) {
        BigDecimal v = new BigDecimal(value);
        PlayerEntity p = PlayerEntity.builder()
                .id(UUID.randomUUID()).name("P").nationalTeam("Spain")
                .position(pos)
                .baseValue(v).currentValue(v)
                .initialMarketValue(v).marketValue(v)
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .active(true).build();
        players.put(p.getId(), p);
        return p;
    }

    private void givePlayerStats(UUID playerId, int minutes, boolean didNotPlay) {
        var stat = PlayerMatchStatsEntity.builder()
                .id(UUID.randomUUID())
                .playerId(playerId).realMatchId(realMatch.getId())
                .minutesPlayed(minutes).didNotPlay(didNotPlay).build();
        // Merge with any existing list-mode stub.
        var current = new ArrayList<PlayerMatchStatsEntity>();
        try {
            current.addAll(statsRepository.findByRealMatchIdIn(List.of(realMatch.getId())));
        } catch (Exception ignored) {}
        current.add(stat);
        when(statsRepository.findByRealMatchIdIn(any())).thenReturn(current);
    }

    private void givePersistedPoints(UUID playerId, int points) {
        PlayerPointsRow row = new PlayerPointsRow() {
            @Override public UUID getPlayerId() { return playerId; }
            @Override public Integer getPoints() { return points; }
        };
        var current = new ArrayList<PlayerPointsRow>();
        try {
            current.addAll(playerScoreRepository.findPointsByMatchday(matchday.getId()));
        } catch (Exception ignored) {}
        current.add(row);
        when(playerScoreRepository.findPointsByMatchday(matchday.getId())).thenReturn(current);
    }
}
