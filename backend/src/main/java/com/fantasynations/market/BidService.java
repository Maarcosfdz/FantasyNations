package com.fantasynations.market;

import com.fantasynations.domain.BidStatus;
import com.fantasynations.domain.MarketCycleStatus;
import com.fantasynations.entity.BidEntity;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.MarketPlayerEntity;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.repository.BidRepository;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.MarketCycleRepository;
import com.fantasynations.repository.MarketPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles submitting and updating SECRET bids. The bidder sees only their own
 * bid; nobody else's amount or existence is exposed by any query in this class.
 * Money is NOT debited at submission time - only at cycle resolution.
 */
@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final MarketPlayerRepository marketPlayerRepository;
    private final MarketCycleRepository cycleRepository;
    private final LeagueMemberRepository leagueMemberRepository;

    /**
     * Place or update the caller's bid on a listing. One bid per (listing, user);
     * resubmissions overwrite the amount and reset {@code submittedAt}.
     */
    @Transactional
    public BidEntity placeBid(UUID leagueId, UUID marketPlayerId, UUID userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Bid amount must be positive");
        }
        var member = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this league"));

        MarketPlayerEntity listing = marketPlayerRepository.findById(marketPlayerId)
                .orElseThrow(() -> new NotFoundException("Listing not found"));
        if (!listing.getLeague().getId().equals(leagueId)) {
            throw new ForbiddenException("Listing belongs to another league");
        }
        if (listing.getCycleId() == null) {
            throw new BadRequestException("Listing is not attached to a cycle");
        }
        MarketCycleEntity cycle = cycleRepository.findById(listing.getCycleId())
                .orElseThrow(() -> new NotFoundException("Cycle not found"));
        if (cycle.getStatus() != MarketCycleStatus.OPEN) {
            throw new BadRequestException("Bidding is closed for this cycle");
        }
        if (cycle.getClosesAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cycle has expired - wait for the next one");
        }
        // Sanity check: at submission time the user must be able to afford the bid.
        // (The real money check happens again at resolution; balances can change.)
        if (member.getMoney().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient funds for this bid");
        }

        Optional<BidEntity> existing = bidRepository
                .findByMarketPlayerIdAndUserId(marketPlayerId, userId);

        BidEntity bid = existing.orElseGet(() -> BidEntity.builder()
                .marketPlayerId(marketPlayerId)
                .userId(userId)
                .cycleId(cycle.getId())
                .build());

        bid.setAmount(amount);
        bid.setStatus(BidStatus.SUBMITTED);
        bid.setSubmittedAt(LocalDateTime.now());
        bid.setResolvedAt(null);
        return bidRepository.save(bid);
    }

    /** Used by the secret-market view: returns only the caller's bid, if any. */
    public Optional<BidEntity> getOwnBid(UUID marketPlayerId, UUID userId) {
        return bidRepository.findByMarketPlayerIdAndUserId(marketPlayerId, userId);
    }
}
