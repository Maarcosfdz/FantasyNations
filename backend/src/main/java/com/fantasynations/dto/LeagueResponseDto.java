package com.fantasynations.dto;

import com.fantasynations.domain.LeagueRules;
import java.time.LocalDateTime;
import java.util.UUID;

public record LeagueResponseDto(
        UUID id,
        String name,
        String inviteCode,
        UUID ownerId,
        String ownerNickname,
        int memberCount,
        LeagueRules rules,
        LocalDateTime createdAt
) {}
