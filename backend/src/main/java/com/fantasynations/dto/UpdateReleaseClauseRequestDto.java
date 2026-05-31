package com.fantasynations.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateReleaseClauseRequestDto(
        @NotNull
        @DecimalMin(value = "0.01", message = "Release clause must be positive")
        BigDecimal releaseClause
) {}
