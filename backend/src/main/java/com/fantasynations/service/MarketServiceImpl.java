package com.fantasynations.service;

import com.fantasynations.dto.MarketPlayerResponseDto;
import com.fantasynations.dto.MarketResponseDto;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.MarketPlayerEntity;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.market.MarketCycleService;
import com.fantasynations.market.MarketListingPopulator;
import com.fantasynations.repository.BidRepository;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.MarketPlayerRepository;
import com.fantasynations.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketServiceImpl implements MarketService {

    static final String REASON_NO_PLAYERS_IN_POOL = "NO_PLAYERS_IN_POOL";
    static final String REASON_NOT_ENOUGH_PLAYERS = "NOT_ENOUGH_PLAYERS";

    private final MarketPlayerRepository marketPlayerRepository;
    private final LeagueRepository leagueRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final BidRepository bidRepository;
    private final MarketCycleService cycleService;
    private final MarketListingPopulator listingPopulator;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public MarketResponseDto getMarket(UUID leagueId, UUID userId) {
        if (!leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)) {
            throw new ForbiddenException("Not a member of this league");
        }
        initializeMarketIfMissing(leagueId);

        MarketCycleEntity cycle = cycleService.getOrCreateOpenCycle(leagueId);
        List<MarketPlayerEntity> active = marketPlayerRepository.findByCycleId(cycle.getId());

        List<MarketPlayerResponseDto> dtos = active.stream()
                .map(m -> toDto(m, userId))
                .collect(Collectors.toList());

        LocalDateTime nextRefreshAt = active.stream()
                .map(MarketPlayerEntity::getAvailableUntil)
                .min(Comparator.naturalOrder())
                .orElse(cycle.getClosesAt());

        String reason = null;
        if (dtos.isEmpty()) {
            reason = REASON_NO_PLAYERS_IN_POOL;
        } else {
            int requested = leagueRepository.findById(leagueId)
                    .map(l -> l.getRules().getMarketPlayersCount())
                    .orElse(dtos.size());
            if (dtos.size() < requested) {
                reason = REASON_NOT_ENOUGH_PLAYERS;
            }
        }
        return new MarketResponseDto(true, nextRefreshAt, dtos, reason);
    }

    @Override
    @Transactional
    public void initializeMarketIfMissing(UUID leagueId) {
        MarketCycleEntity cycle = cycleService.getOrCreateOpenCycle(leagueId);
        long existing = marketPlayerRepository.findByCycleId(cycle.getId()).size();
        if (existing > 0) {
            log.debug("Market already populated for league {} cycle {} ({} listings).",
                    leagueId, cycle.getCycleNumber(), existing);
            return;
        }
        log.info("Populating initial market for league {} cycle {}.", leagueId, cycle.getCycleNumber());
        listingPopulator.populateForCycle(cycle);
    }

    private MarketPlayerResponseDto toDto(MarketPlayerEntity m, UUID viewerUserId) {
        var p = m.getPlayer();
        BigDecimal ownBid = bidRepository
                .findByMarketPlayerIdAndUserId(m.getId(), viewerUserId)
                .map(b -> b.getAmount())
                .orElse(null);
        String sellerNickname = null;
        if (m.getSellerUserId() != null) {
            sellerNickname = userRepository.findById(m.getSellerUserId())
                    .map(u -> u.getNickname())
                    .orElse(null);
        }
        return new MarketPlayerResponseDto(
                m.getId(), p.getId(), p.getName(), p.getNationalTeam(),
                p.getPosition(), p.getImageRef(), m.getPrice(),
                p.getMarketValue(), m.getAvailableUntil(), ownBid,
                m.getSellerUserId(), sellerNickname
        );
    }
}
