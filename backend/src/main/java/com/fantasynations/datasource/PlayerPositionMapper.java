package com.fantasynations.datasource;

import com.fantasynations.domain.PlayerPosition;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Maps the position labels found in the World Cup JSON to the canonical
 * {@link PlayerPosition} enum. Supports English short codes (GK/DF/MF/FW),
 * Spanish names (Portero/Defensa/Defensor/Centrocampista/Medio/Delantero),
 * and the legacy duplicated "DefensorDefensor" value found in the bundled file.
 */
public final class PlayerPositionMapper {

    private static final Map<String, PlayerPosition> MAPPINGS = Map.ofEntries(
            Map.entry("gk", PlayerPosition.GK),
            Map.entry("df", PlayerPosition.DEF),
            Map.entry("mf", PlayerPosition.MID),
            Map.entry("fw", PlayerPosition.FWD),
            Map.entry("portero", PlayerPosition.GK),
            Map.entry("defensa", PlayerPosition.DEF),
            Map.entry("defensor", PlayerPosition.DEF),
            Map.entry("defensordefensor", PlayerPosition.DEF),
            Map.entry("centrocampista", PlayerPosition.MID),
            Map.entry("medio", PlayerPosition.MID),
            Map.entry("delantero", PlayerPosition.FWD)
    );

    private PlayerPositionMapper() {}

    public static Optional<PlayerPosition> map(String raw) {
        if (raw == null) return Optional.empty();
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(MAPPINGS.get(key));
    }
}
