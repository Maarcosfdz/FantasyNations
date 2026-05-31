package com.fantasynations.controller;

import com.fantasynations.dto.RankingEntryDto;
import com.fantasynations.leaderboard.LeaderboardService;
import com.fantasynations.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leagues/{leagueId}/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final LeaderboardService leaderboardService;
    private final AuthenticatedUserProvider userProvider;

    @GetMapping
    public ResponseEntity<List<RankingEntryDto>> getRanking(@PathVariable UUID leagueId) {
        return ResponseEntity.ok(leaderboardService.getLeagueRanking(leagueId, userProvider.getCurrentUserId()));
    }
}
