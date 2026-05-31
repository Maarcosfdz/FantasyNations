package com.fantasynations.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RankingEntryDto(
        int rank,
        UUID userId,
        String nickname,
        String avatarUrl,
        int totalPoints,
        /** Sum of {@code marketValue} of all players currently owned by this
         *  user in this league. Distinct from {@code totalPoints} - this is
         *  money-shaped, not fantasy points. */
        BigDecimal squadValue
) {}
