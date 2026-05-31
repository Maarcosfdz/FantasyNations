package com.fantasynations.dto;

import com.fantasynations.domain.PlayerPosition;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SquadPlayerResponseDto(
        UUID squadPlayerId,
        UUID playerId,
        String playerName,
        String nationalTeam,
        PlayerPosition position,
        String imageRef,
        BigDecimal currentValue,
        BigDecimal releaseClause,
        LocalDateTime protectedUntil,
        LocalDateTime acquiredAt
) {}
