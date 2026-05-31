package com.fantasynations.marketvalue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the optional manual override seed at
 * {@code app.players.market-value-seed-path}. Keys are {@code "Name|Team"}.
 * Missing file is fine - the system just falls back to the automatic
 * algorithm for every player.
 */
@Component
@Slf4j
public class MarketValueSeedLoader {

    private final ObjectMapper objectMapper;

    @Value("${app.players.market-value-seed-path:../fant/market-value-seed.json}")
    private String seedPath;

    private Map<String, MarketValueSeedEntry> cache;

    public MarketValueSeedLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized Map<String, MarketValueSeedEntry> load() {
        if (cache != null) return cache;
        cache = readFromDisk(Paths.get(seedPath));
        return cache;
    }

    /** Useful for tests that want to point at a temp file. */
    public Map<String, MarketValueSeedEntry> loadFrom(Path path) {
        return readFromDisk(path);
    }

    public Optional<MarketValueSeedEntry> findFor(String name, String nationalTeam) {
        return Optional.ofNullable(load().get(name + "|" + nationalTeam));
    }

    private Map<String, MarketValueSeedEntry> readFromDisk(Path path) {
        if (path == null || !Files.exists(path) || !Files.isReadable(path)) {
            log.info("Market value seed file not found at '{}' - using automatic values only.",
                    path == null ? "(null)" : path.toAbsolutePath());
            return Map.of();
        }
        try {
            return objectMapper.readValue(
                    path.toFile(),
                    new TypeReference<Map<String, MarketValueSeedEntry>>() {}
            );
        } catch (IOException e) {
            log.error("Failed to parse market value seed at '{}': {}", path, e.getMessage());
            return Map.of();
        }
    }

    /** Test seam: clears the cached file so the next load() reads from disk. */
    public synchronized void invalidate() {
        cache = null;
    }
}
