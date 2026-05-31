package com.fantasynations.scoring.dto;

import com.fantasynations.domain.Matchday7Slot;

/**
 * Tournament-level context for a player in a specific matchday. Kept separate
 * from {@link PerformanceStats} so the scoring engine is unaware of tournament
 * shape and can be reused outside a World Cup context.
 *
 *   - eliminated: player's team was eliminated before this matchday.
 *   - qualifiedForRound: player's team participates in this matchday's round.
 *   - matchday7Slot: only relevant for matchday 7; controls whether the player
 *     can score at all when the matchday number is 7.
 */
public record MatchdayEligibility(
        boolean eliminated,
        boolean qualifiedForRound,
        Matchday7Slot matchday7Slot
) {
    public static MatchdayEligibility eligible() {
        return new MatchdayEligibility(false, true, Matchday7Slot.NONE);
    }

    public boolean canScore(int matchdayNumber) {
        if (eliminated) return false;
        if (!qualifiedForRound) return false;
        if (matchdayNumber == 7) return matchday7Slot != Matchday7Slot.NONE;
        return true;
    }
}
