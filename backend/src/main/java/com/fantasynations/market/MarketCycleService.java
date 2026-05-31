package com.fantasynations.market;

import com.fantasynations.domain.MarketCycleStatus;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.MarketCycleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the lifecycle of {@link MarketCycleEntity}: creating cycle 1 for a new
 * league, finding the currently open cycle, and creating the next cycle after
 * a resolution. Does NOT decide bid winners - that is
 * {@link MarketCycleResolutionService}'s job.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketCycleService {

    private final MarketCycleRepository cycleRepository;
    private final LeagueRepository leagueRepository;

    /**
     * Returns the currently open cycle for a league, creating cycle 1 if no
     * cycle exists yet. Idempotent.
     */
    @Transactional
    public MarketCycleEntity getOrCreateOpenCycle(UUID leagueId) {
        Optional<MarketCycleEntity> open = cycleRepository
                .findFirstByLeagueIdAndStatusOrderByCycleNumberDesc(leagueId, MarketCycleStatus.OPEN);
        if (open.isPresent()) return open.get();

        // No open cycle - if any cycles already exist we can't create cycle 1.
        Optional<MarketCycleEntity> last = cycleRepository
                .findFirstByLeagueIdOrderByCycleNumberDesc(leagueId);
        if (last.isPresent()) {
            // A previous cycle exists but is not OPEN. The resolver should have
            // created the next one already; if it didn't, do it now defensively.
            return createNextCycle(last.get());
        }
        return createFirstCycle(leagueRepository.findById(leagueId)
                .orElseThrow(() -> new IllegalArgumentException("League not found: " + leagueId)));
    }

    @Transactional
    public MarketCycleEntity createFirstCycle(LeagueEntity league) {
        LocalDateTime opens = LocalDateTime.now();
        int hours = league.getRules().getMarketRefreshIntervalHours();
        var cycle = MarketCycleEntity.builder()
                .leagueId(league.getId())
                .cycleNumber(1)
                .opensAt(opens)
                .closesAt(opens.plusHours(hours))
                .status(MarketCycleStatus.OPEN)
                .build();
        var saved = cycleRepository.save(cycle);
        log.info("Created cycle 1 for league {} (closes at {}).", league.getId(), saved.getClosesAt());
        return saved;
    }

    @Transactional
    public MarketCycleEntity createNextCycle(MarketCycleEntity previous) {
        var league = leagueRepository.findById(previous.getLeagueId())
                .orElseThrow(() -> new IllegalStateException("League not found: " + previous.getLeagueId()));
        int hours = league.getRules().getMarketRefreshIntervalHours();
        LocalDateTime opens = previous.getResolvedAt() != null
                ? previous.getResolvedAt()
                : LocalDateTime.now();
        var cycle = MarketCycleEntity.builder()
                .leagueId(previous.getLeagueId())
                .cycleNumber(previous.getCycleNumber() + 1)
                .opensAt(opens)
                .closesAt(opens.plusHours(hours))
                .status(MarketCycleStatus.OPEN)
                .build();
        var saved = cycleRepository.save(cycle);
        log.info("Created cycle {} for league {} (closes at {}).",
                saved.getCycleNumber(), saved.getLeagueId(), saved.getClosesAt());
        return saved;
    }

    public Optional<MarketCycleEntity> findOpenCycle(UUID leagueId) {
        return cycleRepository.findFirstByLeagueIdAndStatusOrderByCycleNumberDesc(
                leagueId, MarketCycleStatus.OPEN);
    }

    public List<MarketCycleEntity> findCyclesDueForResolution(LocalDateTime now) {
        return cycleRepository.findByStatusAndClosesAtBefore(MarketCycleStatus.OPEN, now);
    }
}
