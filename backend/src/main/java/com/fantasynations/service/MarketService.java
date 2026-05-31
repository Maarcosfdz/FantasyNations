package com.fantasynations.service;

import com.fantasynations.dto.MarketResponseDto;

import java.util.UUID;

public interface MarketService {
    MarketResponseDto getMarket(UUID leagueId, UUID userId);

    /**
     * Ensures a market cycle and its listings exist for the league. Safe to
     * call repeatedly; never produces duplicate cycles or listings.
     */
    void initializeMarketIfMissing(UUID leagueId);
}
