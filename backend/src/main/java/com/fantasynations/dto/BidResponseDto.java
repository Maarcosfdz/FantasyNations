package com.fantasynations.dto;

import com.fantasynations.domain.BidStatus;
import com.fantasynations.entity.BidEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BidResponseDto(
        UUID id,
        UUID marketPlayerId,
        BigDecimal amount,
        BidStatus status,
        LocalDateTime submittedAt
) {
    public static BidResponseDto from(BidEntity e) {
        return new BidResponseDto(e.getId(), e.getMarketPlayerId(),
                e.getAmount(), e.getStatus(), e.getSubmittedAt());
    }
}
