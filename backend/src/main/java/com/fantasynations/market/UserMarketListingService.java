package com.fantasynations.market;

import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.MarketPlayerEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.MarketPlayerRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lets a user put one of their squad players on the public market with a
 * chosen asking price. The listing lasts {@link #USER_LISTING_HOURS} hours;
 * any league member can bid on it. At cycle resolution the seller is credited
 * with the winning bid and the player moves to the buyer's squad.
 *
 * Free market (system) listings keep their existing cycle behaviour - this
 * service ONLY creates user listings (seller_user_id is set).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserMarketListingService {

    public static final int USER_LISTING_HOURS = 48;

    private final SquadPlayerRepository squadPlayerRepository;
    private final MarketPlayerRepository marketPlayerRepository;
    private final MarketCycleService cycleService;

    @Transactional
    public MarketPlayerEntity listOnMarket(UUID leagueId, UUID squadPlayerId,
                                           UUID userId, BigDecimal askingPrice) {
        if (askingPrice == null || askingPrice.signum() <= 0) {
            throw new BadRequestException("Asking price must be positive");
        }
        SquadPlayerEntity sp = squadPlayerRepository.findById(squadPlayerId)
                .orElseThrow(() -> new NotFoundException("Squad player not found"));
        if (!sp.getSquad().getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not own this player");
        }
        if (!sp.getSquad().getLeague().getId().equals(leagueId)) {
            throw new ForbiddenException("Player belongs to another league");
        }

        MarketCycleEntity cycle = cycleService.getOrCreateOpenCycle(leagueId);
        LocalDateTime availableUntil = LocalDateTime.now().plusHours(USER_LISTING_HOURS);

        MarketPlayerEntity listing = MarketPlayerEntity.builder()
                .league(sp.getSquad().getLeague())
                .player(sp.getPlayer())
                .price(askingPrice)
                .availableUntil(availableUntil)
                .cycleId(cycle.getId())
                .sellerUserId(userId)
                .build();
        var saved = marketPlayerRepository.save(listing);
        log.info("User listing created: user {} listing {} in league {} at {} until {}.",
                userId, sp.getPlayer().getName(), leagueId, askingPrice, availableUntil);
        return saved;
    }
}
