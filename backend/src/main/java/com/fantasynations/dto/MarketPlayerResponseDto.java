package com.fantasynations.dto;

import com.fantasynations.domain.PlayerPosition;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MarketPlayerResponseDto(
        UUID id,
        UUID playerId,
        String playerName,
        String nationalTeam,
        PlayerPosition position,
        String imageRef,
        BigDecimal price,
        BigDecimal currentValue,
        LocalDateTime availableUntil,
        BigDecimal ownBidAmount,
        /** NULL when the listing is a free-market / system listing. */
        UUID sellerUserId,
        /** Display-friendly seller nickname. NULL for free-market listings. */
        String sellerNickname
) {}
