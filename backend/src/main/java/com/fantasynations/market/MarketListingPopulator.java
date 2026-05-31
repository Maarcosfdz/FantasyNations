package com.fantasynations.market;

import com.fantasynations.domain.ActivityEventType;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.MarketPlayerEntity;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.MarketPlayerRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fantasynations.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the listings for a given market cycle. Extracted from the legacy
 * {@code MarketServiceImpl.refreshMarket} so cycle creation and population
 * are reusable from the resolver and the initial league setup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketListingPopulator {

    private final MarketPlayerRepository marketPlayerRepository;
    private final PlayerRepository playerRepository;
    private final LeagueRepository leagueRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public int populateForCycle(MarketCycleEntity cycle) {
        LeagueEntity league = leagueRepository.findById(cycle.getLeagueId()).orElseThrow();
        int count = league.getRules().getMarketPlayersCount();

        var pool = playerRepository.findByActiveTrue();
        Collections.shuffle(pool);

        var listings = pool.stream()
                .limit(count)
                .map(player -> MarketPlayerEntity.builder()
                        .league(league)
                        .player(player)
                        .price(player.getMarketValue())
                        .availableUntil(cycle.getClosesAt())
                        .cycleId(cycle.getId())
                        .build())
                .collect(Collectors.toList());

        marketPlayerRepository.saveAll(listings);

        activityLogService.log(league, null, ActivityEventType.MARKET_REFRESHED, Map.of(
                "cycleNumber", String.valueOf(cycle.getCycleNumber())
        ));
        if (listings.size() < count) {
            log.warn("Player pool ({}) smaller than market size ({}) for league {} cycle {}.",
                    listings.size(), count, league.getId(), cycle.getCycleNumber());
        }
        return listings.size();
    }
}
