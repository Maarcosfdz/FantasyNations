package com.fantasynations.service;

import com.fantasynations.domain.RealMatchStatus;
import com.fantasynations.entity.MatchdayEntity;
import com.fantasynations.entity.RealMatchEntity;
import com.fantasynations.repository.MatchdayRepository;
import com.fantasynations.repository.RealMatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchdayLockServiceTest {

    private MatchdayRepository matchdayRepository;
    private RealMatchRepository realMatchRepository;
    private MatchdayLockService service;

    @BeforeEach
    void setUp() {
        matchdayRepository = mock(MatchdayRepository.class);
        realMatchRepository = mock(RealMatchRepository.class);
        service = new MatchdayLockService(matchdayRepository, realMatchRepository);
    }

    @Test
    void emptyListProducesEmptyLock() {
        assertThat(service.computeLockAt(List.of())).isEmpty();
    }

    @Test
    void nullListProducesEmptyLock() {
        assertThat(service.computeLockAt((List<RealMatchEntity>) null)).isEmpty();
    }

    @Test
    void lockAtIsTheEarliestKickoff() {
        LocalDateTime t1 = LocalDateTime.of(2026, 6, 1, 12, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 6, 1, 15, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 6, 1, 18, 0);

        Optional<LocalDateTime> lock = service.computeLockAt(List.of(
                match(t2, RealMatchStatus.SCHEDULED),
                match(t1, RealMatchStatus.SCHEDULED),
                match(t3, RealMatchStatus.SCHEDULED)
        ));

        assertThat(lock).contains(t1);
    }

    @Test
    void cancelledAndPostponedMatchesAreIgnored() {
        LocalDateTime early = LocalDateTime.of(2026, 6, 1, 9, 0);
        LocalDateTime later = LocalDateTime.of(2026, 6, 1, 21, 0);

        Optional<LocalDateTime> lock = service.computeLockAt(List.of(
                match(early, RealMatchStatus.CANCELLED),
                match(early, RealMatchStatus.POSTPONED),
                match(later, RealMatchStatus.SCHEDULED)
        ));

        assertThat(lock).contains(later);
    }

    @Test
    void allCancelledMatchesProduceEmptyLock() {
        Optional<LocalDateTime> lock = service.computeLockAt(List.of(
                match(LocalDateTime.now(), RealMatchStatus.CANCELLED)
        ));
        assertThat(lock).isEmpty();
    }

    @Test
    void recalculatePersistsLockOnMatchday() {
        UUID matchdayId = UUID.randomUUID();
        LocalDateTime kickoff = LocalDateTime.of(2026, 6, 14, 17, 0);
        MatchdayEntity matchday = MatchdayEntity.builder().id(matchdayId).number(1).build();

        when(matchdayRepository.findById(matchdayId)).thenReturn(Optional.of(matchday));
        when(realMatchRepository.findByMatchdayId(matchdayId))
                .thenReturn(List.of(match(kickoff, RealMatchStatus.SCHEDULED)));

        Optional<LocalDateTime> result = service.recalculate(matchdayId);

        assertThat(result).contains(kickoff);
        assertThat(matchday.getLockAt()).isEqualTo(kickoff);
        verify(matchdayRepository).save(any(MatchdayEntity.class));
    }

    @Test
    void recalculateThrowsWhenMatchdayMissing() {
        UUID missing = UUID.randomUUID();
        when(matchdayRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recalculate(missing))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(missing.toString());
    }

    private RealMatchEntity match(LocalDateTime kickoff, RealMatchStatus status) {
        return RealMatchEntity.builder()
                .id(UUID.randomUUID())
                .kickoff(kickoff)
                .homeTeam("A")
                .awayTeam("B")
                .status(status)
                .build();
    }
}
