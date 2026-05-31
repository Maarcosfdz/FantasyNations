package com.fantasynations.controller;

import com.fantasynations.dto.ActivityEntryDto;
import com.fantasynations.security.AuthenticatedUserProvider;
import com.fantasynations.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/leagues/{leagueId}/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityLogService activityLogService;
    private final AuthenticatedUserProvider userProvider;

    @GetMapping
    public ResponseEntity<List<ActivityEntryDto>> getActivity(@PathVariable UUID leagueId) {
        // Verify membership implicitly via the service
        return ResponseEntity.ok(activityLogService.getLeagueActivity(leagueId));
    }
}
