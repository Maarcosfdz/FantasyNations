package com.fantasynations.service;

import com.fantasynations.dto.LineupPlayerDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface LineupService {
    List<LineupPlayerDto> getLineup(UUID leagueId, UUID userId);
    void saveLineup(UUID leagueId, UUID userId, Map<UUID, String> playerSlotMap);
}
