package com.fantasynations.market;

import com.fantasynations.domain.ActivityEventType;
import com.fantasynations.domain.MachineOfferStatus;
import com.fantasynations.entity.MachineOfferEntity;
import com.fantasynations.entity.MarketCycleEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.exception.BadRequestException;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.marketvalue.MarketValueConfig;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.MachineOfferRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import com.fantasynations.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

/**
 * Generates and accepts machine offers (the "computer" buying back a player
 * the user has listed for sale). Amount is deterministic per (squadPlayer,
 * cycle): the same input always produces the same offer, which keeps tests
 * stable while still feeling random to end users.
 *
 *   offer = marketValue * (1 + (hash(squadPlayer.id, cycle.id) -> [-10%, +10%]))
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MachineOfferService {

    private final MachineOfferRepository offerRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final LeagueRepository leagueRepository;
    private final MarketCycleService cycleService;
    private final MarketValueConfig marketValueConfig;
    private final ActivityLogService activityLogService;

    @Transactional
    public MachineOfferEntity listForSale(UUID leagueId, UUID squadPlayerId, UUID userId) {
        var squadPlayer = squadPlayerRepository.findById(squadPlayerId)
                .orElseThrow(() -> new NotFoundException("Squad player not found"));
        if (!squadPlayer.getSquad().getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not own this player");
        }
        if (!squadPlayer.getSquad().getLeague().getId().equals(leagueId)) {
            throw new ForbiddenException("Player belongs to another league");
        }
        // Cancel any prior pending offer on this squadPlayer.
        offerRepository.findBySquadPlayerIdAndStatus(squadPlayerId, MachineOfferStatus.PENDING)
                .ifPresent(prev -> {
                    prev.setStatus(MachineOfferStatus.EXPIRED);
                    offerRepository.save(prev);
                });

        MarketCycleEntity cycle = cycleService.getOrCreateOpenCycle(leagueId);
        BigDecimal amount = computeOfferAmount(squadPlayer, cycle);

        var offer = MachineOfferEntity.builder()
                .leagueId(leagueId)
                .cycleId(cycle.getId())
                .squadPlayerId(squadPlayerId)
                .sellerUserId(userId)
                .amount(amount)
                .status(MachineOfferStatus.PENDING)
                .expiresAt(cycle.getClosesAt())
                .build();
        return offerRepository.save(offer);
    }

    @Transactional
    public MachineOfferEntity accept(UUID leagueId, UUID offerId, UUID userId) {
        var offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found"));
        if (!offer.getLeagueId().equals(leagueId)) {
            throw new ForbiddenException("Offer belongs to another league");
        }
        if (!offer.getSellerUserId().equals(userId)) {
            throw new ForbiddenException("Only the seller can accept this offer");
        }
        if (offer.getStatus() != MachineOfferStatus.PENDING) {
            throw new BadRequestException("Offer is no longer pending");
        }
        var squadPlayer = squadPlayerRepository.findById(offer.getSquadPlayerId())
                .orElseThrow(() -> new NotFoundException("Squad player not found"));
        var member = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this league"));

        member.setMoney(member.getMoney().add(offer.getAmount()));
        leagueMemberRepository.save(member);

        squadPlayerRepository.delete(squadPlayer);
        offer.setStatus(MachineOfferStatus.ACCEPTED);
        offer.setAcceptedAt(java.time.LocalDateTime.now());
        var saved = offerRepository.save(offer);

        var league = leagueRepository.findById(leagueId).orElseThrow();
        activityLogService.log(league, member.getUser(), ActivityEventType.PLAYER_SOLD,
                Map.of(
                        "playerName", squadPlayer.getPlayer().getName(),
                        "price", offer.getAmount().toString(),
                        "kind", "MACHINE_OFFER"
                ));
        return saved;
    }

    /**
     * Deterministic per (squadPlayer, cycle): the same input always produces
     * the same offer. The hash is mapped to {@code [-rangePct, +rangePct]}.
     */
    BigDecimal computeOfferAmount(SquadPlayerEntity squadPlayer, MarketCycleEntity cycle) {
        BigDecimal marketValue = squadPlayer.getPlayer().getMarketValue();
        BigDecimal rangePct = marketValueConfig.machineOfferRangePct; // e.g. 10

        long hash = 1125899906842597L; // FNV-ish prime
        hash = mix(hash, squadPlayer.getId().getMostSignificantBits());
        hash = mix(hash, squadPlayer.getId().getLeastSignificantBits());
        hash = mix(hash, cycle.getId().getMostSignificantBits());
        hash = mix(hash, cycle.getId().getLeastSignificantBits());

        // Map hash to a fraction in [-1, +1]. floorMod keeps the modulus
        // non-negative when hash is negative (Java's % does not).
        double normalized = (double) Math.floorMod(hash, 200_001L) / 100_000.0 - 1.0;
        BigDecimal swingPct = rangePct.multiply(BigDecimal.valueOf(normalized));

        BigDecimal multiplier = BigDecimal.ONE
                .add(swingPct.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        BigDecimal raw = marketValue.multiply(multiplier);
        return raw.setScale(0, RoundingMode.HALF_UP);
    }

    private static long mix(long h, long v) {
        h ^= v;
        h *= 0x100000001b3L; // FNV prime
        return h;
    }
}
