package com.fantasynations.scoring;

import com.fantasynations.domain.RealMatchStatus;
import com.fantasynations.entity.MatchdayEntity;
import com.fantasynations.entity.RealMatchEntity;
import com.fantasynations.marketvalue.DynamicMarketValueService;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.MatchdayRepository;
import com.fantasynations.repository.RealMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Periodically aggregates any matchday whose real matches are all FINISHED
 * for every league that exists. After aggregation completes, runs the
 * dynamic market-value update so player marketValue follows real performance.
 * Idempotent: re-running re-aggregates and reuses snapshots/history rows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchdayAggregationRunner {

    private final MatchdayRepository matchdayRepository;
    private final RealMatchRepository realMatchRepository;
    private final LeagueRepository leagueRepository;
    private final MatchdayAggregationService aggregationService;
    private final DynamicMarketValueService dynamicMarketValueService;

    @Scheduled(fixedDelayString = "${app.scoring.aggregation-interval-ms:300000}")
    @Transactional
    public void runDue() {
        for (MatchdayEntity md : matchdayRepository.findAll()) {
            List<RealMatchEntity> matches = realMatchRepository.findByMatchdayId(md.getId());
            if (matches.isEmpty()) continue;
            boolean allFinished = matches.stream()
                    .allMatch(m -> m.getStatus() == RealMatchStatus.FINISHED);
            if (!allFinished) continue;

            for (var league : leagueRepository.findAll()) {
                try {
                    aggregationService.aggregateAllMembers(league.getId(), md.getId());
                } catch (Exception e) {
                    log.error("Auto-aggregation failed for league {} matchday {}: {}",
                            league.getId(), md.getId(), e.getMessage(), e);
                }
            }
            try {
                dynamicMarketValueService.applyForMatchday(md.getId());
            } catch (Exception e) {
                log.error("Dynamic market-value update failed for matchday {}: {}",
                        md.getId(), e.getMessage(), e);
            }
        }
    }
}
