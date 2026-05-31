package com.fantasynations.marketvalue;

import com.fantasynations.domain.Importance;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Owns the release-clause rules from the spec.
 *
 *   effectiveReleaseClause = max(marketValue, autoClause, fixedReleaseClauseValue?)
 *   autoClause             = marketValue * autoClauseMultiplier(importance)
 *   fixedReleaseClauseValue never decreases automatically.
 *
 * Pure-ish: only reads from / writes to the entity in {@code recalculate}; the
 * computational helpers are static-style and trivial to unit-test.
 */
@Service
public class ReleaseClauseService {

    private final MarketValueConfig config;

    public ReleaseClauseService(MarketValueConfig config) {
        this.config = config;
    }

    public BigDecimal autoClause(BigDecimal marketValue, Importance importance) {
        BigDecimal multiplier = importance == null
                ? config.defaultAutoClauseMultiplier
                : config.autoClauseMultiplier.getOrDefault(importance, config.defaultAutoClauseMultiplier);
        return marketValue.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal effectiveClause(BigDecimal marketValue,
                                      Importance importance,
                                      BigDecimal fixedReleaseClauseValue) {
        BigDecimal auto = autoClause(marketValue, importance);
        BigDecimal eff = marketValue.max(auto);
        if (fixedReleaseClauseValue != null) {
            eff = eff.max(fixedReleaseClauseValue);
        }
        return eff;
    }

    /**
     * Validates a manual raise request. Throws {@link IllegalArgumentException}
     * if the requested value is at or below the current effective clause -
     * the spec forbids both lowering and "no-op" raises.
     */
    public void validateManualRaise(BigDecimal requested,
                                    BigDecimal marketValue,
                                    Importance importance,
                                    BigDecimal currentFixedReleaseClauseValue) {
        if (requested == null) throw new IllegalArgumentException("Requested clause is required");
        if (requested.compareTo(marketValue) < 0) {
            throw new IllegalArgumentException("Clause cannot be below market value");
        }
        BigDecimal current = effectiveClause(marketValue, importance, currentFixedReleaseClauseValue);
        if (requested.compareTo(current) <= 0) {
            throw new IllegalArgumentException(
                    "Clause must be strictly greater than current effective clause");
        }
    }

    /**
     * Recomputes and writes the effective clause on {@code squadPlayer}.
     * Call after market value updates and after a manual raise.
     */
    public BigDecimal recalculate(SquadPlayerEntity squadPlayer) {
        PlayerEntity p = squadPlayer.getPlayer();
        BigDecimal effective = effectiveClause(
                p.getMarketValue(),
                p.getImportance(),
                squadPlayer.getFixedReleaseClauseValue());
        squadPlayer.setReleaseClause(effective);
        return effective;
    }

    /**
     * Applies a manual raise: stores {@code requested} on the squad player and
     * recomputes the effective clause. Caller is responsible for billing the
     * user and persisting the entity.
     */
    public BigDecimal applyManualRaise(SquadPlayerEntity squadPlayer, BigDecimal requested) {
        PlayerEntity p = squadPlayer.getPlayer();
        validateManualRaise(requested, p.getMarketValue(), p.getImportance(),
                squadPlayer.getFixedReleaseClauseValue());
        squadPlayer.setFixedReleaseClauseValue(requested);
        squadPlayer.setReleaseClauseManuallyRaised(true);
        return recalculate(squadPlayer);
    }
}
