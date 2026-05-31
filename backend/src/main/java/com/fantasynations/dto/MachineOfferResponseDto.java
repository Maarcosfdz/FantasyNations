package com.fantasynations.dto;

import com.fantasynations.domain.MachineOfferStatus;
import com.fantasynations.entity.MachineOfferEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MachineOfferResponseDto(
        UUID id,
        UUID squadPlayerId,
        BigDecimal amount,
        MachineOfferStatus status,
        LocalDateTime expiresAt
) {
    public static MachineOfferResponseDto from(MachineOfferEntity e) {
        return new MachineOfferResponseDto(e.getId(), e.getSquadPlayerId(),
                e.getAmount(), e.getStatus(), e.getExpiresAt());
    }
}
