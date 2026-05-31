package com.fantasynations.controller;

import com.fantasynations.dto.SquadPlayerResponseDto;
import com.fantasynations.dto.UpdateReleaseClauseRequestDto;
import com.fantasynations.security.AuthenticatedUserProvider;
import com.fantasynations.service.SquadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leagues/{leagueId}/squad")
@RequiredArgsConstructor
public class SquadController {

    private final SquadService squadService;
    private final AuthenticatedUserProvider userProvider;

    @GetMapping
    public ResponseEntity<List<SquadPlayerResponseDto>> getSquad(@PathVariable UUID leagueId) {
        return ResponseEntity.ok(squadService.getSquad(leagueId, userProvider.getCurrentUserId()));
    }

    @PostMapping("/clause/{playerId}")
    public ResponseEntity<Void> payClause(@PathVariable UUID leagueId, @PathVariable UUID playerId) {
        squadService.payReleaseClause(leagueId, playerId, userProvider.getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{playerId}/clause")
    public ResponseEntity<SquadPlayerResponseDto> updateClause(
            @PathVariable UUID leagueId,
            @PathVariable UUID playerId,
            @Valid @RequestBody UpdateReleaseClauseRequestDto request
    ) {
        return ResponseEntity.ok(squadService.updateReleaseClause(
                leagueId, playerId, userProvider.getCurrentUserId(), request.releaseClause()
        ));
    }
}
