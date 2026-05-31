package com.fantasynations.controller;

import com.fantasynations.dto.BidResponseDto;
import com.fantasynations.dto.ListOnMarketRequestDto;
import com.fantasynations.dto.MachineOfferResponseDto;
import com.fantasynations.dto.MarketPlayerResponseDto;
import com.fantasynations.dto.MarketResponseDto;
import com.fantasynations.dto.PlaceBidRequestDto;
import com.fantasynations.entity.MarketPlayerEntity;
import com.fantasynations.market.BidService;
import com.fantasynations.market.MachineOfferService;
import com.fantasynations.market.QuickSellService;
import com.fantasynations.market.UserMarketListingService;
import com.fantasynations.security.AuthenticatedUserProvider;
import com.fantasynations.service.MarketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/leagues/{leagueId}")
@RequiredArgsConstructor
public class MarketController {

    private final MarketService marketService;
    private final BidService bidService;
    private final MachineOfferService machineOfferService;
    private final QuickSellService quickSellService;
    private final UserMarketListingService userMarketListingService;
    private final AuthenticatedUserProvider userProvider;

    @GetMapping("/market")
    public ResponseEntity<MarketResponseDto> getMarket(@PathVariable UUID leagueId) {
        return ResponseEntity.ok(marketService.getMarket(leagueId, userProvider.getCurrentUserId()));
    }

    /** Submit or update a secret bid on a listing. Money is debited at cycle resolution. */
    @PostMapping("/market/listings/{listingId}/bid")
    public ResponseEntity<BidResponseDto> placeBid(
            @PathVariable UUID leagueId,
            @PathVariable UUID listingId,
            @Valid @RequestBody PlaceBidRequestDto request) {
        var bid = bidService.placeBid(leagueId, listingId,
                userProvider.getCurrentUserId(), request.amount());
        return ResponseEntity.ok(BidResponseDto.from(bid));
    }

    /** List one of your squad players for sale; the system returns a machine offer (market value +/- 10%). */
    @PostMapping("/squad/{squadPlayerId}/list-for-sale")
    public ResponseEntity<MachineOfferResponseDto> listForSale(
            @PathVariable UUID leagueId,
            @PathVariable UUID squadPlayerId) {
        var offer = machineOfferService.listForSale(leagueId, squadPlayerId,
                userProvider.getCurrentUserId());
        return ResponseEntity.ok(MachineOfferResponseDto.from(offer));
    }

    @PostMapping("/offers/{offerId}/accept")
    public ResponseEntity<MachineOfferResponseDto> acceptOffer(
            @PathVariable UUID leagueId,
            @PathVariable UUID offerId) {
        var offer = machineOfferService.accept(leagueId, offerId,
                userProvider.getCurrentUserId());
        return ResponseEntity.ok(MachineOfferResponseDto.from(offer));
    }

    /**
     * Put a squad player on the public market with a chosen asking price.
     * The listing lasts 48h; any league member can bid on it. Sellers are
     * credited when the bid resolves.
     */
    @PostMapping("/squad/{squadPlayerId}/list-on-market")
    public ResponseEntity<MarketPlayerResponseDto> listOnMarket(
            @PathVariable UUID leagueId,
            @PathVariable UUID squadPlayerId,
            @org.springframework.web.bind.annotation.RequestBody ListOnMarketRequestDto request) {
        MarketPlayerEntity listing = userMarketListingService.listOnMarket(
                leagueId, squadPlayerId, userProvider.getCurrentUserId(), request.askingPrice());
        var p = listing.getPlayer();
        return ResponseEntity.ok(new MarketPlayerResponseDto(
                listing.getId(), p.getId(), p.getName(), p.getNationalTeam(),
                p.getPosition(), p.getImageRef(), listing.getPrice(),
                p.getMarketValue(), listing.getAvailableUntil(), null,
                listing.getSellerUserId(), null
        ));
    }

    /** Sells a squad player immediately at quick-sell percent of their global market value. */
    @PostMapping("/squad/{squadPlayerId}/quick-sell")
    public ResponseEntity<QuickSellService.QuickSellResult> quickSell(
            @PathVariable UUID leagueId,
            @PathVariable UUID squadPlayerId) {
        return ResponseEntity.ok(quickSellService.quickSell(leagueId, squadPlayerId,
                userProvider.getCurrentUserId()));
    }
}
