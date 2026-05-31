package com.fantasynations.domain;

import lombok.Data;
import java.math.BigDecimal;

/**
 * League-level rules. The "standard FantasyNations start" is mandatory and is
 * NOT user-configurable: every user receives 15 random players and the
 * remaining cash up to {@link #startingBudget}. Admins must not be able to
 * pick an arbitrary starting money value.
 */
@Data
public class LeagueRules {
    /** Total budget every user gets at league join: players + remaining money. */
    private BigDecimal startingBudget = new BigDecimal("300000000");

    /**
     * @deprecated Kept only so existing leagues whose serialized rules JSON
     *             still contains "startingMoney" can be read without errors.
     *             New code must NOT read it; user money is computed as
     *             {@code startingBudget - assignedSquadMarketValue}.
     */
    @Deprecated
    private BigDecimal startingMoney = new BigDecimal("300000000");

    private BigDecimal moneyPerPoint = new BigDecimal("100000");
    private int releaseClauseProtectionHours = 24;
    private int marketRefreshIntervalHours = 24;
    private int marketPlayersCount = 15;
    private int maxPlayersPerSquad = 25;
    private int minLineupPlayers = 11;
    private boolean formationRulesEnabled = false;

    // ----- Initial squad assignment (mandatory standard start) -----

    /** Total squad size each user receives on creation/join. */
    private int initialSquadSize = 15;

    /** Target squad market value range. The assigner actively tries to land
     *  in this band by biasing toward higher-tier players and using a greedy
     *  fallback when random samples fail. */
    private BigDecimal initialSquadTargetMinValue = new BigDecimal("200000000");
    private BigDecimal initialSquadTargetMaxValue = new BigDecimal("250000000");

    /** Standard composition: 2 GK, 5 DEF, 5 MID, 3 FWD. ±1 fallback on
     *  outfield positions; GK stays at 2 when possible; total stays at 15. */
    private int initialSquadGk  = 2;
    private int initialSquadDef = 5;
    private int initialSquadMid = 5;
    private int initialSquadFwd = 3;
}
