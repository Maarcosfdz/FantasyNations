package com.fantasynations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * The standard FantasyNations start (15 random players + remaining cash up to
 * the global budget) is mandatory and not configurable. Only league-shape
 * settings remain on the request - never starting money / squad value.
 */
public record CreateLeagueRequestDto(
        @NotBlank @Size(min = 2, max = 128) String name,
        BigDecimal moneyPerPoint,
        Integer releaseClauseProtectionHours,
        Integer marketRefreshIntervalHours,
        Integer marketPlayersCount
) {}
