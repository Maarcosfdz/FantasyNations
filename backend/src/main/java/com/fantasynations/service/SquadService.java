package com.fantasynations.service;

import com.fantasynations.dto.SquadPlayerResponseDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SquadService {
    List<SquadPlayerResponseDto> getSquad(UUID leagueId, UUID userId);
    void payReleaseClause(UUID leagueId, UUID playerId, UUID buyerId);
    SquadPlayerResponseDto updateReleaseClause(UUID leagueId, UUID playerId, UUID userId, BigDecimal newClause);
}
