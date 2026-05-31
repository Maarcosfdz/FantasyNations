package com.fantasynations.datasource;

import com.fantasynations.domain.PlayerPosition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerPositionMapperTest {

    @ParameterizedTest
    @CsvSource({
            "GK, GK",
            "gk, GK",
            "DF, DEF",
            "df, DEF",
            "MF, MID",
            "FW, FWD",
            "Portero, GK",
            "Defensa, DEF",
            "Defensor, DEF",
            "DefensorDefensor, DEF",
            "Centrocampista, MID",
            "Medio, MID",
            "Delantero, FWD"
    })
    void mapsKnownLabels(String raw, PlayerPosition expected) {
        assertThat(PlayerPositionMapper.map(raw)).contains(expected);
    }

    @ParameterizedTest
    @CsvSource({"' '", "XYZ", "guardian"})
    void unknownReturnsEmpty(String raw) {
        assertThat(PlayerPositionMapper.map(raw)).isEmpty();
    }

    @org.junit.jupiter.api.Test
    void nullAndEmptyReturnEmpty() {
        assertThat(PlayerPositionMapper.map(null)).isEqualTo(Optional.<PlayerPosition>empty());
        assertThat(PlayerPositionMapper.map("")).isEmpty();
    }
}
