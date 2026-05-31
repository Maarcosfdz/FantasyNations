package com.fantasynations.controller;

import com.fantasynations.domain.LeagueRules;
import com.fantasynations.dto.*;
import com.fantasynations.security.AuthenticatedUserProvider;
import com.fantasynations.service.LeagueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leagues")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueService leagueService;
    private final AuthenticatedUserProvider userProvider;

    @PostMapping
    public ResponseEntity<LeagueResponseDto> createLeague(@Valid @RequestBody CreateLeagueRequestDto request) {
        UUID userId = userProvider.getCurrentUserId();
        return ResponseEntity.ok(leagueService.createLeague(request, userId));
    }

    @PostMapping("/join")
    public ResponseEntity<LeagueResponseDto> joinLeague(@Valid @RequestBody JoinLeagueRequestDto request) {
        UUID userId = userProvider.getCurrentUserId();
        return ResponseEntity.ok(leagueService.joinLeague(request, userId));
    }

    @GetMapping
    public ResponseEntity<List<LeagueResponseDto>> getUserLeagues() {
        UUID userId = userProvider.getCurrentUserId();
        return ResponseEntity.ok(leagueService.getUserLeagues(userId));
    }

    @GetMapping("/{leagueId}")
    public ResponseEntity<LeagueResponseDto> getLeague(@PathVariable UUID leagueId) {
        UUID userId = userProvider.getCurrentUserId();
        return ResponseEntity.ok(leagueService.getLeague(leagueId, userId));
    }

    @GetMapping("/{leagueId}/me")
    public ResponseEntity<LeagueMemberMeResponseDto> getMyMembership(@PathVariable UUID leagueId) {
        UUID userId = userProvider.getCurrentUserId();
        return ResponseEntity.ok(leagueService.getCurrentMember(leagueId, userId));
    }

    @PutMapping("/{leagueId}/settings")
    public ResponseEntity<LeagueResponseDto> updateSettings(
            @PathVariable UUID leagueId,
            @RequestBody LeagueRules rules
    ) {
        UUID userId = userProvider.getCurrentUserId();
        return ResponseEntity.ok(leagueService.updateLeagueSettings(leagueId, rules, userId));
    }
}
