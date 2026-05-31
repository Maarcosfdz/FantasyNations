package com.fantasynations.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceBidRequestDto(
        @NotNull BigDecimal amount
) {}
