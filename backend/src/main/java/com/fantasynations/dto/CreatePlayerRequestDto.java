package com.fantasynations.dto;

import com.fantasynations.domain.PlayerPosition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreatePlayerRequestDto(
        @NotBlank String name,
        @NotBlank String nationalTeam,
        @NotNull PlayerPosition position,
        @Positive BigDecimal baseValue,
        String imageRef
) {}
