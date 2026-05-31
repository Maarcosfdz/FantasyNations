package com.fantasynations.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ListOnMarketRequestDto(
        @NotNull @Positive BigDecimal askingPrice
) {}
