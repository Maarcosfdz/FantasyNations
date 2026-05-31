package com.fantasynations.marketvalue;

import com.fantasynations.domain.Importance;
import com.fantasynations.domain.LeagueReputation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MarketValueSeedLoaderTest {

    private MarketValueSeedLoader loader;

    @BeforeEach
    void setUp() {
        loader = new MarketValueSeedLoader(new ObjectMapper());
    }

    @Test
    void missingFileReturnsEmpty(@TempDir Path tmp) {
        Map<String, MarketValueSeedEntry> map = loader.loadFrom(tmp.resolve("does-not-exist.json"));
        assertThat(map).isEmpty();
    }

    @Test
    void parsesValidJson(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("seed.json");
        Files.writeString(file, """
                {
                  "Kylian Mbappe|France": {
                    "importance": "GLOBAL_SUPERSTAR",
                    "leagueReputation": "ELITE",
                    "initialValueOverride": 70000000
                  },
                  "Luka Modric|Croatia": {
                    "importance": "STAR",
                    "leagueReputation": "ELITE"
                  }
                }
                """);

        Map<String, MarketValueSeedEntry> map = loader.loadFrom(file);

        assertThat(map).hasSize(2);
        MarketValueSeedEntry mbappe = map.get("Kylian Mbappe|France");
        assertThat(mbappe.importance()).isEqualTo(Importance.GLOBAL_SUPERSTAR);
        assertThat(mbappe.leagueReputation()).isEqualTo(LeagueReputation.ELITE);
        assertThat(mbappe.initialValueOverride()).isEqualByComparingTo(new BigDecimal("70000000"));

        MarketValueSeedEntry modric = map.get("Luka Modric|Croatia");
        assertThat(modric.importance()).isEqualTo(Importance.STAR);
        assertThat(modric.initialValueOverride()).isNull();
    }

    @Test
    void malformedJsonIsHandledGracefully(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("seed.json");
        Files.writeString(file, "{not valid json");
        assertThat(loader.loadFrom(file)).isEmpty();
    }
}
