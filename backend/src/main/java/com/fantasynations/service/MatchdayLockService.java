package com.fantasynations.service;

import com.fantasynations.entity.MatchdayEntity;
import com.fantasynations.entity.RealMatchEntity;
import com.fantasynations.repository.MatchdayRepository;
import com.fantasynations.repository.RealMatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Computes the matchday lock timestamp as the kickoff of the earliest real match
 * in the matchday. Cancelled and postponed matches are ignored.
 */
@Service
public class MatchdayLockService {

    private final MatchdayRepository matchdayRepository;
    private final RealMatchRepository realMatchRepository;

    public MatchdayLockService(MatchdayRepository matchdayRepository,
                               RealMatchRepository realMatchRepository) {
        this.matchdayRepository = matchdayRepository;
        this.realMatchRepository = realMatchRepository;
    }

    public Optional<LocalDateTime> computeLockAt(UUID matchdayId) {
        return computeLockAt(realMatchRepository.findByMatchdayId(matchdayId));
    }

    public Optional<LocalDateTime> computeLockAt(List<RealMatchEntity> matches) {
        if (matches == null || matches.isEmpty()) return Optional.empty();
        return matches.stream()
                .filter(MatchdayLockService::countsForLock)
                .map(RealMatchEntity::getKickoff)
                .filter(java.util.Objects::nonNull)
                .min(Comparator.naturalOrder());
    }

    @Transactional
    public Optional<LocalDateTime> recalculate(UUID matchdayId) {
        MatchdayEntity matchday = matchdayRepository.findById(matchdayId)
                .orElseThrow(() -> new IllegalArgumentException("Matchday not found: " + matchdayId));
        Optional<LocalDateTime> lockAt = computeLockAt(matchdayId);
        matchday.setLockAt(lockAt.orElse(null));
        matchdayRepository.save(matchday);
        return lockAt;
    }

    private static boolean countsForLock(RealMatchEntity m) {
        return switch (m.getStatus()) {
            case CANCELLED, POSTPONED -> false;
            default -> true;
        };
    }
}
