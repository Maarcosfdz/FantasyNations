package com.fantasynations.dto;

import com.fantasynations.domain.LeagueRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LeagueMemberMeResponseDto(
        UUID userId,
        UUID leagueId,
        BigDecimal money,
        LeagueRole role,
        LocalDateTime joinedAt
) {}
