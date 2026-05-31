package com.fantasynations.dto;

import com.fantasynations.domain.PlayerPosition;
import java.math.BigDecimal;
import java.util.UUID;

public record PlayerResponseDto(
        UUID id,
        String name,
        String nationalTeam,
        PlayerPosition position,
        BigDecimal baseValue,
        BigDecimal currentValue,
        String imageRef,
        boolean active
) {}
