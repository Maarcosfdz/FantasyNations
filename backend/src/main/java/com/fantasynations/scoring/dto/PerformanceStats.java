package com.fantasynations.scoring.dto;

import com.fantasynations.domain.PlayerPosition;

public record PerformanceStats(
        PlayerPosition position,
        int minutesPlayed,
        boolean didNotPlay,
        int onPitchGoalsConceded,
        boolean teamCleanSheet,
        MatchEvents events,
        ShootoutEvents shootout,
        OptionalStats optional
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private PlayerPosition position;
        private int minutesPlayed;
        private boolean didNotPlay;
        private int onPitchGoalsConceded;
        private boolean teamCleanSheet;
        private MatchEvents events = MatchEvents.empty();
        private ShootoutEvents shootout = ShootoutEvents.empty();
        private OptionalStats optional = OptionalStats.empty();

        public Builder position(PlayerPosition v)        { this.position = v; return this; }
        public Builder minutesPlayed(int v)               { this.minutesPlayed = v; return this; }
        public Builder didNotPlay(boolean v)              { this.didNotPlay = v; return this; }
        public Builder onPitchGoalsConceded(int v)        { this.onPitchGoalsConceded = v; return this; }
        public Builder teamCleanSheet(boolean v)          { this.teamCleanSheet = v; return this; }
        public Builder events(MatchEvents v)              { this.events = v; return this; }
        public Builder shootout(ShootoutEvents v)         { this.shootout = v; return this; }
        public Builder optional(OptionalStats v)          { this.optional = v; return this; }

        public PerformanceStats build() {
            return new PerformanceStats(position, minutesPlayed, didNotPlay,
                    onPitchGoalsConceded, teamCleanSheet, events, shootout, optional);
        }
    }
}
