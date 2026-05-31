package com.fantasynations.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Market endpoint response. {@code available} signals the market exists for this
 * league (the caller can render the market view); {@code reason} explains why
 * the {@code players} list might be shorter than the league's configured size.
 *
 * Possible {@code reason} values:
 *   - null: full market populated as configured.
 *   - "NO_PLAYERS_IN_POOL": the player pool is empty; nothing could be drawn.
 *   - "NOT_ENOUGH_PLAYERS": fewer players were available than the rules ask for.
 */
public record MarketResponseDto(
        boolean available,
        LocalDateTime nextRefreshAt,
        List<MarketPlayerResponseDto> players,
        String reason
) {}
