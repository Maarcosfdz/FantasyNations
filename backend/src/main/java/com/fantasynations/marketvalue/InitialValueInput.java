package com.fantasynations.marketvalue;

import com.fantasynations.domain.Importance;
import com.fantasynations.domain.LeagueReputation;
import com.fantasynations.domain.PlayerPosition;

import java.math.BigDecimal;

public record InitialValueInput(
        PlayerPosition position,
        String nationalTeam,
        Importance importance,
        LeagueReputation leagueReputation,
        BigDecimal manualStarBonus,
        BigDecimal initialValueOverride
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private PlayerPosition position;
        private String nationalTeam;
        private Importance importance;
        private LeagueReputation leagueReputation;
        private BigDecimal manualStarBonus;
        private BigDecimal initialValueOverride;
        public Builder position(PlayerPosition v) { this.position = v; return this; }
        public Builder nationalTeam(String v) { this.nationalTeam = v; return this; }
        public Builder importance(Importance v) { this.importance = v; return this; }
        public Builder leagueReputation(LeagueReputation v) { this.leagueReputation = v; return this; }
        public Builder manualStarBonus(BigDecimal v) { this.manualStarBonus = v; return this; }
        public Builder initialValueOverride(BigDecimal v) { this.initialValueOverride = v; return this; }
        public InitialValueInput build() {
            return new InitialValueInput(position, nationalTeam, importance,
                    leagueReputation, manualStarBonus, initialValueOverride);
        }
    }
}
