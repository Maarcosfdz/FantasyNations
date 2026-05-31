package com.fantasynations.scoring;

import com.fantasynations.entity.LineupEntity;
import com.fantasynations.entity.LockedLineupPlayerEntity;
import com.fantasynations.repository.LockedLineupPlayerRepository;
import com.fantasynations.repository.LineupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creates a frozen snapshot of a user's lineup the first time a matchday is
 * aggregated. After the snapshot is created:
 *
 *   - the user may keep editing their LIVE lineup freely;
 *   - later edits, transfers, clauses, market purchases and quick-sells must
 *     NOT affect this matchday's score;
 *   - if a player was in the snapshot, he still scores even after leaving the
 *     squad;
 *   - re-aggregating the same matchday reuses the existing snapshot and does
 *     not create another one.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineupFreezeService {

    private final LockedLineupPlayerRepository lockedLineupRepository;
    private final LineupRepository lineupRepository;

    /**
     * Returns the snapshot for {@code (lineup, matchday)}, creating it from
     * the live lineup the first time it's requested.
     */
    @Transactional
    public List<LockedLineupPlayerEntity> getOrFreeze(LineupEntity lineup, UUID matchdayId) {
        var existing = lockedLineupRepository.findByLineupIdAndMatchdayId(lineup.getId(), matchdayId);
        if (!existing.isEmpty()) {
            return existing;
        }

        var snapshot = lineup.getPlayers().stream()
                .map(lp -> LockedLineupPlayerEntity.builder()
                        .lineupId(lineup.getId())
                        .matchdayId(matchdayId)
                        .leagueId(lineup.getLeague().getId())
                        .userId(lineup.getUser().getId())
                        .playerId(lp.getPlayer().getId())
                        .positionSlot(lp.getPositionSlot())
                        .build())
                .collect(Collectors.toList());

        if (snapshot.isEmpty()) {
            log.info("Lineup {} is empty at lock time for matchday {} - snapshot will be empty.",
                    lineup.getId(), matchdayId);
        }

        var saved = lockedLineupRepository.saveAll(snapshot);

        // Stamp the matchday on the lineup so the FE can show "frozen for matchday N".
        lineup.setFrozenAt(LocalDateTime.now());
        lineup.setFrozenForMatchdayId(matchdayId);
        lineupRepository.save(lineup);

        log.info("Froze lineup {} for matchday {} ({} players).",
                lineup.getId(), matchdayId, saved.size());
        return saved;
    }

    public List<LockedLineupPlayerEntity> findSnapshot(UUID lineupId, UUID matchdayId) {
        return lockedLineupRepository.findByLineupIdAndMatchdayId(lineupId, matchdayId);
    }
}
