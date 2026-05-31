package com.fantasynations.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FormationTest {

    @ParameterizedTest
    @CsvSource({
            // GK, DEF, MID, FWD
            "1, 3, 4, 3",
            "1, 3, 5, 2",
            "1, 3, 6, 1",
            "1, 4, 3, 3",
            "1, 4, 4, 2",
            "1, 4, 5, 1",
            "1, 5, 2, 3",
            "1, 5, 3, 2",
            "1, 5, 4, 1"
    })
    void allListedFormationsAreValid(int gk, int def, int mid, int fwd) {
        assertThat(Formation.isValid(gk, def, mid, fwd)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            // < 3 DEF
            "1, 2, 5, 3",
            "1, 1, 6, 3",
            // > 5 DEF
            "1, 6, 3, 1",
            // < 2 MID
            "1, 5, 1, 4",
            // > 6 MID
            "1, 3, 7, 0",
            // < 1 FWD
            "1, 5, 5, 0",
            // > 3 FWD
            "1, 3, 3, 4",
            // wrong GK count
            "0, 4, 4, 3",
            "2, 4, 4, 1",
            // wrong total
            "1, 4, 4, 3",
            "1, 4, 4, 1"
    })
    void invalidShapesAreRejected(int gk, int def, int mid, int fwd) {
        assertThat(Formation.isValid(gk, def, mid, fwd)).isFalse();
    }

    @Test
    void allFormationsListedExposeReadableCodes() {
        assertThat(Formation.ALL)
                .extracting(Formation::code)
                .containsExactlyInAnyOrder(
                        "1-3-4-3", "1-3-5-2", "1-3-6-1",
                        "1-4-3-3", "1-4-4-2", "1-4-5-1",
                        "1-5-2-3", "1-5-3-2", "1-5-4-1");
    }
}
