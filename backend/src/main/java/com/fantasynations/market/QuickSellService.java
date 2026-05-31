package com.fantasynations.market;

import com.fantasynations.domain.ActivityEventType;
import com.fantasynations.exception.ForbiddenException;
import com.fantasynations.exception.NotFoundException;
import com.fantasynations.marketvalue.MarketValueConfig;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.LeagueRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import com.fantasynations.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

/**
 * Immediate sell-back to the system at {@link MarketValueConfig#quickSellPercent}
 * (default 50%) of the player's global market value. No bidding, no waiting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuickSellService {

    private final SquadPlayerRepository squadPlayerRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final LeagueRepository leagueRepository;
    private final MarketValueConfig config;
    private final ActivityLogService activityLogService;

    public record QuickSellResult(UUID squadPlayerId, BigDecimal amountCredited) {}

    @Transactional
    public QuickSellResult quickSell(UUID leagueId, UUID squadPlayerId, UUID userId) {
        var squadPlayer = squadPlayerRepository.findById(squadPlayerId)
                .orElseThrow(() -> new NotFoundException("Squad player not found"));
        if (!squadPlayer.getSquad().getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not own this player");
        }
        if (!squadPlayer.getSquad().getLeague().getId().equals(leagueId)) {
            throw new ForbiddenException("Player belongs to another league");
        }
        var member = leagueMemberRepository.findByLeagueIdAndUserId(leagueId, userId)
                .orElseThrow(() -> new ForbiddenException("Not a member of this league"));

        BigDecimal marketValue = squadPlayer.getPlayer().getMarketValue();
        BigDecimal payout = marketValue
                .multiply(config.quickSellPercent)
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        member.setMoney(member.getMoney().add(payout));
        leagueMemberRepository.save(member);

        squadPlayerRepository.delete(squadPlayer);

        var league = leagueRepository.findById(leagueId).orElseThrow();
        activityLogService.log(league, member.getUser(), ActivityEventType.PLAYER_SOLD,
                Map.of(
                        "playerName", squadPlayer.getPlayer().getName(),
                        "price", payout.toString(),
                        "kind", "QUICK_SELL"
                ));
        log.info("Quick sell: user {} sold {} for {} in league {}.",
                userId, squadPlayer.getPlayer().getName(), payout, leagueId);
        return new QuickSellResult(squadPlayerId, payout);
    }
}
