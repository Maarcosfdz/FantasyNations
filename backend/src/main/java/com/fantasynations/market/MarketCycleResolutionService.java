package com.fantasynations.market;

import com.fantasynations.domain.ActivityEventType;
import com.fantasynations.domain.BidStatus;
import com.fantasynations.domain.MachineOfferStatus;
import com.fantasynations.domain.MarketCycleStatus;
import com.fantasynations.entity.BidEntity;
import com.fantasynations.entity.LeagueMemberEntity;
import com.fantasynations.entity.MachineOfferEntity;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.MarketPlayerEntity;
import com.fantasynations.entity.SquadEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.marketvalue.ReleaseClauseService;
import com.fantasynations.repository.BidRepository;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.MachineOfferRepository;
import com.fantasynations.repository.MarketCycleRepository;
import com.fantasynations.repository.MarketPlayerRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import com.fantasynations.repository.SquadRepository;
import com.fantasynations.repository.UserRepository;
import com.fantasynations.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Closes a market cycle: walks each listing, picks the highest bid (earliest
 * submission breaks ties), tries to debit the winner, transfers the player,
 * marks losers/rejected bids, expires unaccepted machine offers, then opens
 * the next cycle and populates fresh listings.
 *
 *   - Money is debited at resolution, never at bid submission.
 *   - If the winner can't pay, the bid is REJECTED_NO_FUNDS and the next-highest
 *     bid gets the chance.
 *   - Cross-league listings are never affected: every query is scoped to the
 *     cycle being resolved.
 *   - Re-running the resolver on a CLOSED cycle is a no-op.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketCycleResolutionService {

    private final MarketCycleRepository cycleRepository;
    private final MarketPlayerRepository marketPlayerRepository;
    private final BidRepository bidRepository;
    private final MachineOfferRepository machineOfferRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final SquadRepository squadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final UserRepository userRepository;
    private final MarketCycleService cycleService;
    private final MarketListingPopulator listingPopulator;
    private final ReleaseClauseService releaseClauseService;
    private final ActivityLogService activityLogService;

    public record ResolutionResult(
            UUID cycleId,
            int listingsResolved,
            int transfersExecuted,
            int rejectedNoFunds,
            UUID nextCycleId
    ) {}

    @Transactional
    public ResolutionResult resolve(UUID cycleId) {
        MarketCycleEntity cycle = cycleRepository.findById(cycleId).orElseThrow();
        if (cycle.getStatus() == MarketCycleStatus.CLOSED) {
            log.debug("Cycle {} already closed.", cycleId);
            return new ResolutionResult(cycleId, 0, 0, 0, null);
        }
        cycle.setStatus(MarketCycleStatus.RESOLVING);
        cycleRepository.save(cycle);

        List<MarketPlayerEntity> listings = marketPlayerRepository.findByCycleId(cycleId);
        int transfers = 0;
        int rejected = 0;

        // User listings can outlive the cycle they were created in (48h vs 24h).
        // Skip those whose availableUntil is still in the future; they will be
        // re-anchored to the next cycle further down.
        List<MarketPlayerEntity> liveUserListings = new java.util.ArrayList<>();

        for (MarketPlayerEntity listing : listings) {
            if (listing.getSellerUserId() != null
                    && listing.getAvailableUntil() != null
                    && listing.getAvailableUntil().isAfter(LocalDateTime.now())) {
                liveUserListings.add(listing);
                continue;
            }
            List<BidEntity> bids = bidRepository
                    .findByMarketPlayerIdAndStatusOrderByAmountDescSubmittedAtAsc(
                            listing.getId(), BidStatus.SUBMITTED);
            BidEntity winner = null;
            for (BidEntity candidate : bids) {
                LeagueMemberEntity member = leagueMemberRepository
                        .findByLeagueIdAndUserId(listing.getLeague().getId(), candidate.getUserId())
                        .orElse(null);
                if (member == null || member.getMoney().compareTo(candidate.getAmount()) < 0) {
                    candidate.setStatus(BidStatus.REJECTED_NO_FUNDS);
                    candidate.setResolvedAt(LocalDateTime.now());
                    bidRepository.save(candidate);
                    rejected++;
                    continue;
                }
                winner = candidate;
                break;
            }
            if (winner != null) {
                executeTransfer(listing, winner);
                transfers++;
                // Mark every other submitted bid on this listing as LOST.
                for (BidEntity other : bids) {
                    if (!other.getId().equals(winner.getId())
                            && other.getStatus() == BidStatus.SUBMITTED) {
                        other.setStatus(BidStatus.LOST);
                        other.setResolvedAt(LocalDateTime.now());
                        bidRepository.save(other);
                    }
                }
            }
            // Listing always gets removed at the end of its cycle; unsold ones
            // disappear and a fresh batch is drawn for the next cycle.
            marketPlayerRepository.delete(listing);
        }

        // Expire any machine offers that were not accepted.
        for (MachineOfferEntity offer : machineOfferRepository
                .findByCycleIdAndStatus(cycleId, MachineOfferStatus.PENDING)) {
            offer.setStatus(MachineOfferStatus.EXPIRED);
            machineOfferRepository.save(offer);
        }

        cycle.setStatus(MarketCycleStatus.CLOSED);
        cycle.setResolvedAt(LocalDateTime.now());
        cycleRepository.save(cycle);

        MarketCycleEntity nextCycle = cycleService.createNextCycle(cycle);
        listingPopulator.populateForCycle(nextCycle);

        // Re-anchor live user listings to the new cycle so they remain visible
        // and biddable until their own 48h availableUntil expires.
        for (MarketPlayerEntity userListing : liveUserListings) {
            userListing.setCycleId(nextCycle.getId());
            marketPlayerRepository.save(userListing);
        }

        log.info("Resolved cycle {} (league {}): {} listings, {} transfers, {} rejected. " +
                        "Next cycle {} opened.",
                cycle.getCycleNumber(), cycle.getLeagueId(), listings.size(),
                transfers, rejected, nextCycle.getCycleNumber());
        return new ResolutionResult(cycleId, listings.size(), transfers, rejected, nextCycle.getId());
    }

    private void executeTransfer(MarketPlayerEntity listing, BidEntity winner) {
        LeagueMemberEntity buyer = leagueMemberRepository
                .findByLeagueIdAndUserId(listing.getLeague().getId(), winner.getUserId())
                .orElseThrow();
        buyer.setMoney(buyer.getMoney().subtract(winner.getAmount()));
        leagueMemberRepository.save(buyer);

        // User listing: credit the seller and remove their existing ownership.
        UUID sellerId = listing.getSellerUserId();
        if (sellerId != null) {
            LeagueMemberEntity seller = leagueMemberRepository
                    .findByLeagueIdAndUserId(listing.getLeague().getId(), sellerId)
                    .orElse(null);
            if (seller != null) {
                seller.setMoney(seller.getMoney().add(winner.getAmount()));
                leagueMemberRepository.save(seller);
            }
            SquadEntity sellerSquad = squadRepository
                    .findByLeagueIdAndUserId(listing.getLeague().getId(), sellerId)
                    .orElse(null);
            if (sellerSquad != null) {
                squadPlayerRepository.findBySquadIdAndPlayerId(
                        sellerSquad.getId(), listing.getPlayer().getId()
                ).ifPresent(squadPlayerRepository::delete);
            }
        }

        SquadEntity buyerSquad = squadRepository
                .findByLeagueIdAndUserId(listing.getLeague().getId(), winner.getUserId())
                .orElseThrow();

        SquadPlayerEntity ownership = SquadPlayerEntity.builder()
                .squad(buyerSquad)
                .player(listing.getPlayer())
                .releaseClause(BigDecimal.ZERO) // recalculated next
                .build();
        releaseClauseService.recalculate(ownership);
        squadPlayerRepository.save(ownership);

        winner.setStatus(BidStatus.WON);
        winner.setResolvedAt(LocalDateTime.now());
        bidRepository.save(winner);

        activityLogService.log(
                listing.getLeague(),
                userRepository.findById(winner.getUserId()).orElseThrow(),
                ActivityEventType.PLAYER_BOUGHT,
                Map.of(
                        "playerName", listing.getPlayer().getName(),
                        "price", winner.getAmount().toString(),
                        "kind", "BID_WON"
                ));
    }

    @Scheduled(fixedDelayString = "${app.market.resolution-interval-ms:60000}")
    @Transactional
    public void resolveDueCycles() {
        var due = cycleService.findCyclesDueForResolution(LocalDateTime.now());
        for (var cycle : due) {
            try {
                resolve(cycle.getId());
            } catch (Exception e) {
                log.error("Failed to resolve cycle {}: {}", cycle.getId(), e.getMessage(), e);
            }
        }
    }
}
