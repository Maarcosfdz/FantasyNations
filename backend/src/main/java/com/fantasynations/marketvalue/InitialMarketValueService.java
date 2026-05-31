package com.fantasynations.marketvalue;

import com.fantasynations.domain.Importance;
import com.fantasynations.domain.MarketValueChangeReason;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.MarketValueHistoryEntity;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.repository.MarketValueHistoryRepository;
import com.fantasynations.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Applies the initial market value algorithm to a player and persists the
 * resulting {@link MarketValueHistoryEntity} entry. Importance fallback (by
 * lineup order per team / position) is deterministic and computed against
 * counters held on this service instance for a single import run.
 *
 * The caller must invoke {@link #beginImportRun()} before importing and use
 * the {@link #applyForNewPlayer(PlayerEntity)} method so each player gets the
 * right STARTER / ROTATION / BENCH fallback.
 */
@Service
@Slf4j
public class InitialMarketValueService {

    private final MarketValueCalculator calculator;
    private final MarketValueConfig config;
    private final MarketValueSeedLoader seedLoader;
    private final PlayerRepository playerRepository;
    private final MarketValueHistoryRepository historyRepository;
    private final ObjectMapper objectMapper;

    /** Per-team, per-position counter used by the importance fallback. */
    private final Map<String, int[]> teamPositionCounter = new HashMap<>();

    public InitialMarketValueService(MarketValueCalculator calculator,
                                     MarketValueConfig config,
                                     MarketValueSeedLoader seedLoader,
                                     PlayerRepository playerRepository,
                                     MarketValueHistoryRepository historyRepository,
                                     ObjectMapper objectMapper) {
        this.calculator = calculator;
        this.config = config;
        this.seedLoader = seedLoader;
        this.playerRepository = playerRepository;
        this.historyRepository = historyRepository;
        this.objectMapper = objectMapper;
    }

    /** Resets fallback counters; call once at the start of an import run. */
    public synchronized void beginImportRun() {
        teamPositionCounter.clear();
    }

    /**
     * Applies the initial-value algorithm to {@code player}, writes the values
     * back on the entity (caller is responsible for saving) and persists one
     * {@link MarketValueHistoryEntity} row with reason {@code INITIAL_VALUE}.
     *
     * Returns the computed value for inspection in tests.
     */
    @Transactional
    public BigDecimal applyForNewPlayer(PlayerEntity player) {
        Optional<MarketValueSeedEntry> seed =
                seedLoader.findFor(player.getName(), player.getNationalTeam());

        Importance importance = seed.map(MarketValueSeedEntry::importance)
                .orElseGet(() -> fallbackImportance(player));

        var input = InitialValueInput.builder()
                .position(player.getPosition())
                .nationalTeam(player.getNationalTeam())
                .importance(importance)
                .leagueReputation(seed.map(MarketValueSeedEntry::leagueReputation).orElse(null))
                .manualStarBonus(seed.map(MarketValueSeedEntry::manualStarBonus).orElse(null))
                .initialValueOverride(seed.map(MarketValueSeedEntry::initialValueOverride).orElse(null))
                .build();

        InitialValueResult result = calculator.calculateInitial(input);

        player.setImportance(importance);
        if (seed.isPresent() && seed.get().leagueReputation() != null) {
            player.setLeagueReputation(seed.get().leagueReputation());
        }
        player.setInitialMarketValue(result.value());
        player.setMarketValue(result.value());
        // Keep legacy fields in sync until the rest of the codebase moves to marketValue.
        player.setBaseValue(result.value());
        player.setCurrentValue(result.value());

        PlayerEntity saved = playerRepository.save(player);

        historyRepository.save(MarketValueHistoryEntity.builder()
                .playerId(saved.getId())
                .oldValue(BigDecimal.ZERO)
                .newValue(result.value())
                .delta(result.value())
                .deltaPercent(null)
                .momentumScore(null)
                .reason(MarketValueChangeReason.INITIAL_VALUE)
                .breakdownJson(toJson(result.breakdown()))
                .build());

        return result.value();
    }

    /**
     * Deterministic position-based fallback. The first {@code N} players of a
     * given (team, position) are STARTERs, the next bucket are ROTATION, the
     * rest are BENCH. The exact thresholds come from
     * {@link MarketValueConfig#fallbackStartersGK} etc.
     */
    Importance fallbackImportance(PlayerEntity player) {
        String key = player.getNationalTeam() + "|" + player.getPosition().name();
        int[] counter = teamPositionCounter.computeIfAbsent(key, k -> new int[]{0});
        int index = counter[0]++;
        int starterLimit = startersLimit(player.getPosition());
        int rotationLimit = starterLimit + starterLimit; // simple, deterministic
        if (index < starterLimit)  return Importance.STARTER;
        if (index < rotationLimit) return Importance.ROTATION;
        return Importance.BENCH;
    }

    private int startersLimit(PlayerPosition p) {
        return switch (p) {
            case GK  -> config.fallbackStartersGK;
            case DEF -> config.fallbackStartersDEF;
            case MID -> config.fallbackStartersMID;
            case FWD -> config.fallbackStartersFWD;
        };
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize breakdown: {}", e.getMessage());
            return null;
        }
    }
}
