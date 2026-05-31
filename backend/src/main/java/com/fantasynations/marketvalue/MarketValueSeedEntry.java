package com.fantasynations.marketvalue;

import com.fantasynations.domain.Importance;
import com.fantasynations.domain.LeagueReputation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketValueSeedEntry(
        Importance importance,
        LeagueReputation leagueReputation,
        BigDecimal initialValueOverride,
        BigDecimal manualStarBonus
) {}
