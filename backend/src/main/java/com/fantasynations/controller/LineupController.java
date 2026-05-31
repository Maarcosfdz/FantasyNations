package com.fantasynations.controller;

import com.fantasynations.dto.LineupPlayerDto;
import com.fantasynations.security.AuthenticatedUserProvider;
import com.fantasynations.service.LineupService;
import com.fantasynations.validation.Formation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/leagues/{leagueId}/lineup")
@RequiredArgsConstructor
public class LineupController {

    private final LineupService lineupService;
    private final AuthenticatedUserProvider userProvider;

    @GetMapping
    public ResponseEntity<List<LineupPlayerDto>> getLineup(@PathVariable UUID leagueId) {
        return ResponseEntity.ok(lineupService.getLineup(leagueId, userProvider.getCurrentUserId()));
    }

    @PutMapping
    public ResponseEntity<Void> saveLineup(
            @PathVariable UUID leagueId,
            @RequestBody Map<UUID, String> playerSlotMap
    ) {
        lineupService.saveLineup(leagueId, userProvider.getCurrentUserId(), playerSlotMap);
        return ResponseEntity.ok().build();
    }

    /** Codes of the formations the engine accepts. Public so the FE can render
     *  a formation selector without duplicating the list. */
    @GetMapping("/formations")
    public ResponseEntity<List<String>> getFormations(@PathVariable UUID leagueId) {
        return ResponseEntity.ok(Formation.ALL.stream().map(Formation::code).toList());
    }
}
