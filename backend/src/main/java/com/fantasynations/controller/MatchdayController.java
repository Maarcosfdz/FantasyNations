package com.fantasynations.controller;

import com.fantasynations.dto.MatchdayListItemDto;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.MatchdayRepository;
import com.fantasynations.repository.MatchdayScoreRepository;
import com.fantasynations.security.AuthenticatedUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Public read endpoint feeding the lineup screen's matchday selector and the
 * score-mode bar. Returns all matchdays of the World Cup with caller-specific
 * total points where available.
 */
@RestController
@RequestMapping("/api/leagues/{leagueId}/matchdays")
@RequiredArgsConstructor
public class MatchdayController {

    private final MatchdayRepository matchdayRepository;
    private final MatchdayScoreRepository scoreRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final AuthenticatedUserProvider userProvider;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<MatchdayListItemDto>> list(@PathVariable UUID leagueId) {
        UUID userId = userProvider.getCurrentUserId();
        if (!leagueMemberRepository.existsByLeagueIdAndUserId(leagueId, userId)) {
            throw new ForbiddenException("Not a member of this league");
        }
        return ResponseEntity.ok(
                matchdayRepository.findAll().stream()
                        .sorted(Comparator.comparingInt(m -> m.getNumber()))
                        .map(m -> {
                            Integer myPoints = scoreRepository
                                    .findByLeagueIdAndUserIdAndMatchdayId(leagueId, userId, m.getId())
                                    .map(s -> s.getTotalPoints())
                                    .orElse(null);
                            return new MatchdayListItemDto(
                                    m.getId(), m.getNumber(), m.getPhase(),
                                    m.getStatus(), m.getLockAt(), myPoints
                            );
                        })
                        .toList()
        );
    }
}
