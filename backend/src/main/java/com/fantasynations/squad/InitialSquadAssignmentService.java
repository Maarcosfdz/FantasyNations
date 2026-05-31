package com.fantasynations.squad;

import com.fantasynations.domain.LeagueRules;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.LeagueEntity;
import com.fantasynations.entity.LeagueMemberEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.entity.SquadEntity;
import com.fantasynations.entity.SquadPlayerEntity;
import com.fantasynations.marketvalue.ReleaseClauseService;
import com.fantasynations.repository.LeagueMemberRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fantasynations.repository.SquadPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Standard FantasyNations start: every user receives 15 random players plus
 * the remaining cash up to {@code rules.startingBudget}. There is NO
 * "empty squad" or "random squad" choice - this behaviour is mandatory.
 *
 * Algorithm (deterministic given the injected {@link RandomGenerator}):
 *   1. Build composition. Try the standard 2/4/6/3; if any pool is short,
 *      borrow ±1 from another outfield bucket while keeping the total at 15.
 *   2. Try up to {@code MAX_ATTEMPTS} random samples; accept the first squad
 *      whose total market value lands in [target_min, target_max].
 *   3. If no attempt lands in range, keep the attempt with the smallest
 *      distance to the target band that does not exceed the budget.
 *   4. If the best candidate still exceeds {@code startingBudget}, swap the
 *      most-expensive picks for cheaper ones from the same position until it
 *      fits, or until no swap helps (in which case the user simply receives
 *      no cash - never blocks creation).
 *   5. Persist ownership rows, compute the effective release clause, and set
 *      the user's money to {@code startingBudget - squadMarketValue} (>= 0).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InitialSquadAssignmentService {

    private static final int MAX_ATTEMPTS = 200;
    /**
     * When biased sampling is used, each pick comes from the top
     * {@code BIAS_TOP_FRACTION} of the position pool (sorted by marketValue DESC).
     * This pushes the squad toward the target band of 200M-250M instead of
     * filling it with bench filler.
     */
    private static final double BIAS_TOP_FRACTION = 0.65;

    private final PlayerRepository playerRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final LeagueMemberRepository leagueMemberRepository;
    private final ReleaseClauseService releaseClauseService;
    private final RandomGenerator randomGenerator;

    public record AssignmentResult(
            int playersAssigned,
            BigDecimal squadMarketValue,
            BigDecimal startingMoney,
            Map<PlayerPosition, Integer> composition,
            boolean usedFallbackComposition,
            boolean inTargetRange
    ) {}

    @Transactional
    public AssignmentResult assignFor(LeagueEntity league, LeagueMemberEntity member, SquadEntity squad) {
        LeagueRules rules = league.getRules();
        Set<UUID> alreadyAssigned = squadPlayerRepository
                .findAll().stream()
                .filter(sp -> sp.getSquad().getLeague().getId().equals(league.getId()))
                .map(sp -> sp.getPlayer().getId())
                .collect(Collectors.toCollection(HashSet::new));

        Map<PlayerPosition, List<PlayerEntity>> pools = poolsByPosition(alreadyAssigned);

        Composition composition = chooseComposition(rules, pools);
        if (composition == null) {
            log.warn("Not enough players in any composition for league {} - assigning empty squad.", league.getId());
            member.setMoney(rules.getStartingBudget());
            leagueMemberRepository.save(member);
            return new AssignmentResult(0, BigDecimal.ZERO, rules.getStartingBudget(),
                    Map.of(), false, false);
        }

        List<PlayerEntity> bestSquad = pickBestSquad(pools, composition, rules);

        // Final safety: never let the squad exceed the budget if there is any way to avoid it.
        bestSquad = constrainToBudget(bestSquad, pools, composition, rules.getStartingBudget());

        BigDecimal squadValue = sumValue(bestSquad);
        BigDecimal money = rules.getStartingBudget().subtract(squadValue).max(BigDecimal.ZERO);

        persist(squad, bestSquad);
        member.setMoney(money);
        leagueMemberRepository.save(member);

        boolean inRange = squadValue.compareTo(rules.getInitialSquadTargetMinValue()) >= 0
                && squadValue.compareTo(rules.getInitialSquadTargetMaxValue()) <= 0;

        log.info("Assigned initial squad for user {} in league {}: {} players, squadValue={}, money={}.",
                member.getUser().getId(), league.getId(), bestSquad.size(), squadValue, money);

        return new AssignmentResult(bestSquad.size(), squadValue, money,
                composition.toMap(), composition.usedFallback, inRange);
    }

    // ---- composition planning ----

    record Composition(int gk, int def, int mid, int fwd, boolean usedFallback) {
        int total() { return gk + def + mid + fwd; }
        Map<PlayerPosition, Integer> toMap() {
            EnumMap<PlayerPosition, Integer> m = new EnumMap<>(PlayerPosition.class);
            m.put(PlayerPosition.GK,  gk);
            m.put(PlayerPosition.DEF, def);
            m.put(PlayerPosition.MID, mid);
            m.put(PlayerPosition.FWD, fwd);
            return m;
        }
    }

    Composition chooseComposition(LeagueRules rules, Map<PlayerPosition, List<PlayerEntity>> pools) {
        int gk  = rules.getInitialSquadGk();
        int def = rules.getInitialSquadDef();
        int mid = rules.getInitialSquadMid();
        int fwd = rules.getInitialSquadFwd();
        int total = rules.getInitialSquadSize();

        // Standard composition first.
        if (fits(pools, gk, def, mid, fwd)) {
            return new Composition(gk, def, mid, fwd, false);
        }

        // ±1 fallback only on outfield positions. Walk every option that keeps
        // the total at 15 and try to satisfy each pool.
        for (int dGk = 0; dGk <= 0; dGk++) {                  // GK stays fixed when possible
            for (int dDef = -1; dDef <= 1; dDef++) {
                for (int dMid = -1; dMid <= 1; dMid++) {
                    for (int dFwd = -1; dFwd <= 1; dFwd++) {
                        int newGk  = gk + dGk;
                        int newDef = def + dDef;
                        int newMid = mid + dMid;
                        int newFwd = fwd + dFwd;
                        if (newGk + newDef + newMid + newFwd != total) continue;
                        if (newDef < 0 || newMid < 0 || newFwd < 0) continue;
                        if (fits(pools, newGk, newDef, newMid, newFwd)) {
                            return new Composition(newGk, newDef, newMid, newFwd, true);
                        }
                    }
                }
            }
        }

        // GK pool too thin? Allow GK to drop by 1, compensate elsewhere.
        if (pools.get(PlayerPosition.GK).size() < gk) {
            int newGk = Math.max(0, pools.get(PlayerPosition.GK).size());
            int remaining = total - newGk;
            // Distribute remaining roughly in DEF/MID/FWD ratio of the original composition.
            int newDef = def, newMid = mid, newFwd = fwd;
            int extra = remaining - (newDef + newMid + newFwd);
            // Pour the extra into MID (largest bucket), then DEF, then FWD.
            newMid += extra;
            if (fits(pools, newGk, newDef, newMid, newFwd)) {
                return new Composition(newGk, newDef, newMid, newFwd, true);
            }
        }
        return null;
    }

    private boolean fits(Map<PlayerPosition, List<PlayerEntity>> pools,
                         int gk, int def, int mid, int fwd) {
        return pools.get(PlayerPosition.GK).size()  >= gk
            && pools.get(PlayerPosition.DEF).size() >= def
            && pools.get(PlayerPosition.MID).size() >= mid
            && pools.get(PlayerPosition.FWD).size() >= fwd;
    }

    // ---- pick best squad ----

    private List<PlayerEntity> pickBestSquad(Map<PlayerPosition, List<PlayerEntity>> pools,
                                             Composition c, LeagueRules rules) {
        List<PlayerEntity> best = null;
        BigDecimal bestDistance = null;
        BigDecimal min = rules.getInitialSquadTargetMinValue();
        BigDecimal max = rules.getInitialSquadTargetMaxValue();
        BigDecimal budget = rules.getStartingBudget();

        // Pre-sort pools by marketValue DESC once; biased samples draw from the
        // top portion to prevent bench-only squads from being accepted early.
        Map<PlayerPosition, List<PlayerEntity>> biasedPools = new EnumMap<>(PlayerPosition.class);
        for (PlayerPosition pos : PlayerPosition.values()) {
            List<PlayerEntity> sorted = new ArrayList<>(pools.get(pos));
            sorted.sort((a, b) -> b.getMarketValue().compareTo(a.getMarketValue()));
            biasedPools.put(pos, sorted);
        }

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            boolean biased = attempt < MAX_ATTEMPTS / 2; // first half biased, then uniform
            List<PlayerEntity> squad = new ArrayList<>();
            squad.addAll(samplePool(pools, biasedPools, PlayerPosition.GK,  c.gk,  biased));
            squad.addAll(samplePool(pools, biasedPools, PlayerPosition.DEF, c.def, biased));
            squad.addAll(samplePool(pools, biasedPools, PlayerPosition.MID, c.mid, biased));
            squad.addAll(samplePool(pools, biasedPools, PlayerPosition.FWD, c.fwd, biased));
            BigDecimal total = sumValue(squad);

            if (total.compareTo(min) >= 0 && total.compareTo(max) <= 0) {
                return squad;
            }
            BigDecimal distance = total.compareTo(min) < 0
                    ? min.subtract(total)
                    : total.subtract(max);
            if (total.compareTo(budget) <= 0
                    && (bestDistance == null || distance.compareTo(bestDistance) < 0)) {
                bestDistance = distance;
                best = squad;
            } else if (best == null) {
                best = squad;
            }
        }

        // Greedy fallback: pool is rich enough but random missed the band.
        // Start from the TOP-N picks per position and swap the most expensive
        // down until total fits under max. Beats random in low-noise scenarios.
        List<PlayerEntity> greedy = greedyTopPick(biasedPools, c);
        BigDecimal greedyTotal = sumValue(greedy);
        if (greedyTotal.compareTo(max) > 0) {
            greedy = constrainBetween(greedy, pools, min, max);
            greedyTotal = sumValue(greedy);
        }
        if (greedyTotal.compareTo(min) >= 0 && greedyTotal.compareTo(max) <= 0) {
            return greedy;
        }
        // Greedy still misses: prefer whichever candidate has the largest squad value
        // (random "best" or greedy) - we want competitive squads, not weak ones.
        if (best == null || sumValue(greedy).compareTo(sumValue(best)) > 0) {
            best = greedy;
        }
        return best == null ? List.of() : best;
    }

    /** Pick the top N highest-value players from the sorted biased pool. */
    private List<PlayerEntity> greedyTopPick(Map<PlayerPosition, List<PlayerEntity>> biased,
                                             Composition c) {
        List<PlayerEntity> out = new ArrayList<>();
        out.addAll(takeFirstN(biased.get(PlayerPosition.GK),  c.gk));
        out.addAll(takeFirstN(biased.get(PlayerPosition.DEF), c.def));
        out.addAll(takeFirstN(biased.get(PlayerPosition.MID), c.mid));
        out.addAll(takeFirstN(biased.get(PlayerPosition.FWD), c.fwd));
        return out;
    }

    private List<PlayerEntity> takeFirstN(List<PlayerEntity> sorted, int n) {
        return new ArrayList<>(sorted.subList(0, Math.min(n, sorted.size())));
    }

    /**
     * Swap most-expensive picks for cheaper same-position alternatives until
     * total drops into [{@code min}, {@code max}]. Stops when within range or
     * no improvement available.
     */
    private List<PlayerEntity> constrainBetween(List<PlayerEntity> squad,
                                                Map<PlayerPosition, List<PlayerEntity>> pools,
                                                BigDecimal min, BigDecimal max) {
        List<PlayerEntity> mutable = new ArrayList<>(squad);
        Set<UUID> used = mutable.stream().map(PlayerEntity::getId).collect(Collectors.toCollection(HashSet::new));

        boolean improved = true;
        while (sumValue(mutable).compareTo(max) > 0 && improved) {
            improved = false;
            mutable.sort((a, b) -> b.getMarketValue().compareTo(a.getMarketValue()));
            for (PlayerEntity expensive : new ArrayList<>(mutable)) {
                PlayerPosition pos = expensive.getPosition();
                // Pick the most expensive alternative below `expensive` that
                // still keeps total >= min where possible.
                BigDecimal currentTotal = sumValue(mutable);
                PlayerEntity bestSwap = null;
                BigDecimal bestSwapDistance = null;
                for (PlayerEntity candidate : pools.get(pos)) {
                    if (used.contains(candidate.getId())) continue;
                    if (candidate.getMarketValue().compareTo(expensive.getMarketValue()) >= 0) continue;
                    BigDecimal newTotal = currentTotal
                            .subtract(expensive.getMarketValue())
                            .add(candidate.getMarketValue());
                    if (newTotal.compareTo(min) < 0) continue; // would underflow target
                    BigDecimal dist = newTotal.subtract(max).abs();
                    if (bestSwap == null || dist.compareTo(bestSwapDistance) < 0) {
                        bestSwap = candidate;
                        bestSwapDistance = dist;
                    }
                }
                if (bestSwap != null) {
                    used.remove(expensive.getId());
                    used.add(bestSwap.getId());
                    mutable.remove(expensive);
                    mutable.add(bestSwap);
                    improved = true;
                    break;
                }
            }
        }
        return mutable;
    }

    /**
     * Returns {@code n} samples from the pool. When {@code biased}, samples
     * are drawn from the top {@link #BIAS_TOP_FRACTION} of the sorted pool
     * (sorted DESC by marketValue), so the resulting squad skews competitive
     * instead of bench-heavy. When the biased slice is smaller than {@code n},
     * the rest is drawn from the full pool.
     */
    private List<PlayerEntity> samplePool(Map<PlayerPosition, List<PlayerEntity>> pools,
                                          Map<PlayerPosition, List<PlayerEntity>> biased,
                                          PlayerPosition pos, int n, boolean useBias) {
        if (!useBias) return sampleWithoutReplacement(pools.get(pos), n);

        List<PlayerEntity> sorted = biased.get(pos);
        int top = Math.max(n, (int) Math.ceil(sorted.size() * BIAS_TOP_FRACTION));
        List<PlayerEntity> topSlice = new ArrayList<>(sorted.subList(0, Math.min(top, sorted.size())));
        return sampleWithoutReplacement(topSlice, n);
    }

    /** Swaps the most-expensive picks for cheaper alternatives in the same
     *  position until the squad fits within the budget, or no improvement is
     *  available. Never reduces the squad size. */
    private List<PlayerEntity> constrainToBudget(List<PlayerEntity> squad,
                                                 Map<PlayerPosition, List<PlayerEntity>> pools,
                                                 Composition c, BigDecimal budget) {
        if (sumValue(squad).compareTo(budget) <= 0) return squad;

        List<PlayerEntity> mutable = new ArrayList<>(squad);
        Set<UUID> used = mutable.stream().map(PlayerEntity::getId).collect(Collectors.toCollection(HashSet::new));

        boolean improved = true;
        while (sumValue(mutable).compareTo(budget) > 0 && improved) {
            improved = false;
            mutable.sort((a, b) -> b.getMarketValue().compareTo(a.getMarketValue()));
            for (PlayerEntity expensive : new ArrayList<>(mutable)) {
                PlayerPosition pos = expensive.getPosition();
                PlayerEntity cheaper = pools.get(pos).stream()
                        .filter(p -> !used.contains(p.getId()))
                        .filter(p -> p.getMarketValue().compareTo(expensive.getMarketValue()) < 0)
                        .min((a, b) -> a.getMarketValue().compareTo(b.getMarketValue()))
                        .orElse(null);
                if (cheaper != null) {
                    used.remove(expensive.getId());
                    used.add(cheaper.getId());
                    mutable.remove(expensive);
                    mutable.add(cheaper);
                    improved = true;
                    break;
                }
            }
        }
        return mutable;
    }

    private List<PlayerEntity> sampleWithoutReplacement(List<PlayerEntity> source, int n) {
        if (n <= 0 || source.isEmpty()) return List.of();
        List<PlayerEntity> copy = new ArrayList<>(source);
        // Fisher-Yates partial shuffle on the first n positions.
        for (int i = 0; i < n && i < copy.size(); i++) {
            int j = i + randomGenerator.nextInt(copy.size() - i);
            PlayerEntity tmp = copy.get(i);
            copy.set(i, copy.get(j));
            copy.set(j, tmp);
        }
        return new ArrayList<>(copy.subList(0, Math.min(n, copy.size())));
    }

    // ---- persistence ----

    private void persist(SquadEntity squad, List<PlayerEntity> picks) {
        for (PlayerEntity p : picks) {
            SquadPlayerEntity sp = SquadPlayerEntity.builder()
                    .squad(squad)
                    .player(p)
                    .releaseClause(BigDecimal.ZERO)
                    .build();
            releaseClauseService.recalculate(sp);
            squadPlayerRepository.save(sp);
        }
    }

    private Map<PlayerPosition, List<PlayerEntity>> poolsByPosition(Set<UUID> excluded) {
        EnumMap<PlayerPosition, List<PlayerEntity>> map = new EnumMap<>(PlayerPosition.class);
        for (PlayerPosition pos : PlayerPosition.values()) map.put(pos, new ArrayList<>());
        for (PlayerEntity p : playerRepository.findByActiveTrue()) {
            if (excluded.contains(p.getId())) continue;
            map.get(p.getPosition()).add(p);
        }
        return map;
    }

    static BigDecimal sumValue(List<PlayerEntity> players) {
        return players.stream()
                .map(PlayerEntity::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
