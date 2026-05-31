package com.fantasynations.dto;

import com.fantasynations.domain.PlayerPosition;
import java.util.UUID;

public record LineupPlayerDto(
        UUID playerId,
        String playerName,
        String nationalTeam,
        PlayerPosition position,
        String imageRef,
        String positionSlot
) {}
