package com.fantasynations.service;

import com.fantasynations.dto.*;
import com.fantasynations.domain.LeagueRules;
import java.util.List;
import java.util.UUID;

public interface LeagueService {
    LeagueResponseDto createLeague(CreateLeagueRequestDto request, UUID ownerId);
    LeagueResponseDto joinLeague(JoinLeagueRequestDto request, UUID userId);
    List<LeagueResponseDto> getUserLeagues(UUID userId);
    LeagueResponseDto getLeague(UUID leagueId, UUID userId);
    LeagueResponseDto updateLeagueSettings(UUID leagueId, LeagueRules rules, UUID userId);
    LeagueMemberMeResponseDto getCurrentMember(UUID leagueId, UUID userId);
}
