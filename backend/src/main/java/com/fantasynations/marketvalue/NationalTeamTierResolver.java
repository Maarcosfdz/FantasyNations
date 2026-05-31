package com.fantasynations.marketvalue;

import com.fantasynations.domain.NationalTeamTier;
import org.springframework.stereotype.Component;

/**
 * Resolves a national team name to its tier. Unknown teams fall to tier C
 * (zero bonus), which keeps initial values low rather than erroring out.
 */
@Component
public class NationalTeamTierResolver {

    private final MarketValueConfig config;

    public NationalTeamTierResolver(MarketValueConfig config) {
        this.config = config;
    }

    public NationalTeamTier resolve(String nationalTeam) {
        if (nationalTeam == null) return NationalTeamTier.C;
        if (config.tierS.contains(nationalTeam)) return NationalTeamTier.S;
        if (config.tierA.contains(nationalTeam)) return NationalTeamTier.A;
        if (config.tierB.contains(nationalTeam)) return NationalTeamTier.B;
        return NationalTeamTier.C;
    }
}
