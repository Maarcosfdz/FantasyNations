package com.fantasynations.marketvalue;

import com.fantasynations.domain.AvailabilityStatus;
import com.fantasynations.domain.Importance;

import java.math.BigDecimal;

/**
 * Per-cycle input to the dynamic value calculation. All fields are nullable;
 * missing inputs are treated per the spec (e.g. missing minutes contribute 0).
 */
public record DynamicValueInput(
        BigDecimal currentMarketValue,
        Importance importance,
        AvailabilityStatus availability,
        Integer lastMatchdayPoints,
        Double averagePoints,
        Integer minutesPlayed,
        boolean didNotPlay,
        boolean restedSuperstar,
        boolean teamEliminated,
        boolean teamReachedSemiFinals,
        boolean teamReachedFinal,
        Integer demandScore
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private BigDecimal currentMarketValue;
        private Importance importance;
        private AvailabilityStatus availability;
        private Integer lastMatchdayPoints;
        private Double averagePoints;
        private Integer minutesPlayed;
        private boolean didNotPlay;
        private boolean restedSuperstar;
        private boolean teamEliminated;
        private boolean teamReachedSemiFinals;
        private boolean teamReachedFinal;
        private Integer demandScore;

        public Builder currentMarketValue(BigDecimal v) { this.currentMarketValue = v; return this; }
        public Builder importance(Importance v) { this.importance = v; return this; }
        public Builder availability(AvailabilityStatus v) { this.availability = v; return this; }
        public Builder lastMatchdayPoints(Integer v) { this.lastMatchdayPoints = v; return this; }
        public Builder averagePoints(Double v) { this.averagePoints = v; return this; }
        public Builder minutesPlayed(Integer v) { this.minutesPlayed = v; return this; }
        public Builder didNotPlay(boolean v) { this.didNotPlay = v; return this; }
        public Builder restedSuperstar(boolean v) { this.restedSuperstar = v; return this; }
        public Builder teamEliminated(boolean v) { this.teamEliminated = v; return this; }
        public Builder teamReachedSemiFinals(boolean v) { this.teamReachedSemiFinals = v; return this; }
        public Builder teamReachedFinal(boolean v) { this.teamReachedFinal = v; return this; }
        public Builder demandScore(Integer v) { this.demandScore = v; return this; }

        public DynamicValueInput build() {
            return new DynamicValueInput(currentMarketValue, importance, availability,
                    lastMatchdayPoints, averagePoints, minutesPlayed, didNotPlay,
                    restedSuperstar, teamEliminated, teamReachedSemiFinals,
                    teamReachedFinal, demandScore);
        }
    }
}
