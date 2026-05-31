package com.fantasynations.dto;

import com.fantasynations.domain.MatchdayPhase;
import com.fantasynations.domain.MatchdayStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One row of the league's matchday list, enriched with the caller's own
 * total points for that matchday (null if not aggregated yet).
 *
 * Used by the lineup screen to render the matchday selector and the score
 * mode bar.
 */
public record MatchdayListItemDto(
        UUID id,
        int number,
        MatchdayPhase phase,
        MatchdayStatus status,
        LocalDateTime lockAt,
        /** Caller's total points for this matchday. {@code null} if not aggregated. */
        Integer myTotalPoints
) {}
