package com.fantasynations.scoring;

import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.LineupEntity;
import com.fantasynations.entity.LineupPlayerEntity;
import com.fantasynations.entity.LockedLineupPlayerEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.UserEntity;
import com.fantasynations.repository.LineupRepository;
import com.fantasynations.repository.LockedLineupPlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LineupFreezeServiceTest {

    private LockedLineupPlayerRepository lockedRepo;
    private LineupRepository lineupRepo;
    private LineupFreezeService service;
    private LineupEntity lineup;
    private UUID matchdayId;
    private final List<LockedLineupPlayerEntity> store = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lockedRepo = mock(LockedLineupPlayerRepository.class);
        lineupRepo = mock(LineupRepository.class);
        service = new LineupFreezeService(lockedRepo, lineupRepo);

        matchdayId = UUID.randomUUID();
        UUID leagueId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        lineup = LineupEntity.builder()
                .id(UUID.randomUUID())
                .league(LeagueEntity.builder().id(leagueId).build())
                .user(UserEntity.builder().id(userId).build())
                .players(new ArrayList<>(List.of(
                        slot("GK-1"), slot("DEF-1"), slot("DEF-2"), slot("DEF-3"),
                        slot("DEF-4"), slot("MID-1"), slot("MID-2"), slot("MID-3"),
                        slot("FWD-1"), slot("FWD-2"), slot("FWD-3"))))
                .build();

        when(lockedRepo.findByLineupIdAndMatchdayId(eq(lineup.getId()), eq(matchdayId)))
                .thenAnswer(inv -> new ArrayList<>(store));
        when(lockedRepo.saveAll(any())).thenAnswer(inv -> {
            Iterable<LockedLineupPlayerEntity> in = inv.getArgument(0);
            in.forEach(e -> {
                if (e.getId() == null) e.setId(UUID.randomUUID());
                store.add(e);
            });
            return store;
        });
        when(lineupRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void firstFreezeCreatesSnapshotOfTheLiveLineup() {
        var snapshot = service.getOrFreeze(lineup, matchdayId);

        assertThat(snapshot).hasSize(11);
        assertThat(snapshot)
                .extracting(LockedLineupPlayerEntity::getPositionSlot)
                .contains("GK-1", "DEF-1", "MID-1", "FWD-1");
        assertThat(lineup.getFrozenForMatchdayId()).isEqualTo(matchdayId);
        verify(lockedRepo, times(1)).saveAll(any());
    }

    @Test
    void rerunReusesExistingSnapshot() {
        service.getOrFreeze(lineup, matchdayId);

        // Simulate the user editing the live lineup AFTER lock: remove a player.
        lineup.getPlayers().remove(0);

        var snapshot = service.getOrFreeze(lineup, matchdayId);

        assertThat(snapshot).hasSize(11); // snapshot keeps the original 11
        verify(lockedRepo, times(1)).saveAll(any()); // not called twice
    }

    @Test
    void emptyLiveLineupProducesEmptySnapshot() {
        lineup.setPlayers(new ArrayList<>());
        var snapshot = service.getOrFreeze(lineup, matchdayId);

        assertThat(snapshot).isEmpty();
        verify(lockedRepo, times(1)).saveAll(any());
    }

    @Test
    void differentMatchdayCreatesSeparateSnapshot() {
        service.getOrFreeze(lineup, matchdayId);

        UUID other = UUID.randomUUID();
        when(lockedRepo.findByLineupIdAndMatchdayId(eq(lineup.getId()), eq(other)))
                .thenReturn(new ArrayList<>());

        service.getOrFreeze(lineup, other);
        verify(lockedRepo, times(2)).saveAll(any());
    }

    private LineupPlayerEntity slot(String slotId) {
        PlayerEntity p = PlayerEntity.builder()
                .id(UUID.randomUUID()).name("P-" + slotId).nationalTeam("T")
                .position(com.fantasynations.domain.PlayerPosition.MID)
                .baseValue(java.math.BigDecimal.ONE).currentValue(java.math.BigDecimal.ONE)
                .initialMarketValue(java.math.BigDecimal.ONE).marketValue(java.math.BigDecimal.ONE)
                .active(true).build();
        return LineupPlayerEntity.builder()
                .id(UUID.randomUUID()).player(p).positionSlot(slotId).build();
    }
}
