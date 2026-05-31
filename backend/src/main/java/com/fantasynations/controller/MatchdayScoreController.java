package com.fantasynations.controller;

import com.fantasynations.dto.MatchdayScoreResponseDto;
import com.fantasynations.scoring.MatchdayScoreReader;
import com.fantasynations.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/leagues/{leagueId}/matchdays/{matchdayId}/score")
@RequiredArgsConstructor
public class MatchdayScoreController {

    private final MatchdayScoreReader reader;
    private final AuthenticatedUserProvider userProvider;

    /** Caller's own frozen lineup + per-player breakdown for the matchday. */
    @GetMapping
    public ResponseEntity<MatchdayScoreResponseDto> getOwnScore(
            @PathVariable UUID leagueId,
            @PathVariable UUID matchdayId) {
        UUID viewer = userProvider.getCurrentUserId();
        return ResponseEntity.ok(reader.getForUser(leagueId, matchdayId, viewer, viewer));
    }

    /** Another league member's frozen lineup + breakdown for the matchday. */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<MatchdayScoreResponseDto> getUserScore(
            @PathVariable UUID leagueId,
            @PathVariable UUID matchdayId,
            @PathVariable UUID userId) {
        return ResponseEntity.ok(
                reader.getForUser(leagueId, matchdayId, userProvider.getCurrentUserId(), userId));
    }
}
