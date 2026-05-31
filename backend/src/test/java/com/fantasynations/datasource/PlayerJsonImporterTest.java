package com.fantasynations.datasource;

import com.fantasynations.datasource.dto.WorldCupPlayerDto;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.marketvalue.InitialMarketValueService;
import com.fantasynations.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerJsonImporterTest {

    private PlayerRepository repository;
    private PlayerJsonImporter importer;
    private final Map<String, PlayerEntity> store = new HashMap<>();

    @BeforeEach
    void setUp() {
        store.clear();
        repository = mock(PlayerRepository.class);

        // Stubbed InitialMarketValueService: stamps placeholder values so the
        // importer's "insert" branch behaves end-to-end without pulling in the
        // full market-value config + calculator stack. That stack has its own
        // dedicated tests; here we only care that the importer routes new
        // players through it.
        InitialMarketValueService initialService = mock(InitialMarketValueService.class);
        when(initialService.applyForNewPlayer(any(PlayerEntity.class))).thenAnswer(inv -> {
            PlayerEntity p = inv.getArgument(0);
            BigDecimal v = new BigDecimal("1000000");
            p.setBaseValue(v);
            p.setCurrentValue(v);
            p.setInitialMarketValue(v);
            p.setMarketValue(v);
            // The real service persists via the repository - mirror that here
            // so tests can verify(repository).save(...).
            return repository.save(p) != null ? v : v;
        });

        importer = new PlayerJsonImporter(repository, new ObjectMapper(), initialService);

        when(repository.findByNameAndNationalTeam(any(), any())).thenAnswer(inv ->
                Optional.ofNullable(store.get(key(inv.getArgument(0), inv.getArgument(1))))
        );
        when(repository.save(any(PlayerEntity.class))).thenAnswer(inv -> {
            PlayerEntity p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            store.put(key(p.getName(), p.getNationalTeam()), p);
            return p;
        });
    }

    @Test
    void insertsNewPlayersWithMappedPositions() {
        var result = importer.importDtos(List.of(
                new WorldCupPlayerDto("Emiliano Martinez", "Argentina", "GK", "/players/argentina/emiliano-martinez.png"),
                new WorldCupPlayerDto("Cristian Romero",   "Argentina", "DefensorDefensor", "/players/argentina/cristian-romero.png"),
                new WorldCupPlayerDto("Rodrigo De Paul",   "Argentina", "MF", "/players/argentina/rodrigo-de-paul.png"),
                new WorldCupPlayerDto("Lionel Messi",      "Argentina", "FW", "/players/argentina/lionel-messi.png")
        ));

        assertThat(result.inserted()).isEqualTo(4);
        assertThat(result.updated()).isZero();
        assertThat(result.skipped()).isZero();

        ArgumentCaptor<PlayerEntity> captor = ArgumentCaptor.forClass(PlayerEntity.class);
        verify(repository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(PlayerEntity::getName, PlayerEntity::getPosition)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("Emiliano Martinez", PlayerPosition.GK),
                        org.assertj.core.groups.Tuple.tuple("Cristian Romero",   PlayerPosition.DEF),
                        org.assertj.core.groups.Tuple.tuple("Rodrigo De Paul",   PlayerPosition.MID),
                        org.assertj.core.groups.Tuple.tuple("Lionel Messi",      PlayerPosition.FWD)
                );
        assertThat(captor.getAllValues())
                .allSatisfy(p -> {
                    assertThat(p.getBaseValue()).isEqualByComparingTo(new BigDecimal("1000000"));
                    assertThat(p.getCurrentValue()).isEqualByComparingTo(new BigDecimal("1000000"));
                    assertThat(p.isActive()).isTrue();
                });
    }

    @Test
    void rerunIsIdempotentWhenNothingChanged() {
        List<WorldCupPlayerDto> dtos = List.of(
                new WorldCupPlayerDto("Luka Modric", "Croatia", "MF", "/players/croatia/luka-modric.png")
        );

        var first = importer.importDtos(dtos);
        var second = importer.importDtos(dtos);

        assertThat(first.inserted()).isEqualTo(1);
        assertThat(second.inserted()).isZero();
        assertThat(second.updated()).isZero();
        assertThat(second.skipped()).isEqualTo(1);
        verify(repository, times(1)).save(any());
    }

    @Test
    void rerunUpdatesChangedFields() {
        var initial = new WorldCupPlayerDto("Kylian Mbappe", "France", "FW", "/players/france/kylian-mbappe.png");
        importer.importDtos(List.of(initial));

        var changed = new WorldCupPlayerDto("Kylian Mbappe", "France", "MF", "/players/france/kylian-mbappe-2.png");
        var result = importer.importDtos(List.of(changed));

        assertThat(result.updated()).isEqualTo(1);
        assertThat(result.inserted()).isZero();
        PlayerEntity stored = store.get(key("Kylian Mbappe", "France"));
        assertThat(stored.getPosition()).isEqualTo(PlayerPosition.MID);
        assertThat(stored.getImageRef()).isEqualTo("/players/france/kylian-mbappe-2.png");
    }

    @Test
    void acceptsPlayersWithNullImage() {
        var result = importer.importDtos(List.of(
                new WorldCupPlayerDto("Mystery Player", "Brazil", "FW", null)
        ));

        assertThat(result.inserted()).isEqualTo(1);
        PlayerEntity stored = store.get(key("Mystery Player", "Brazil"));
        assertThat(stored.getImageRef()).isNull();
    }

    @Test
    void skipsRowsWithMissingNameOrUnknownPosition() {
        var result = importer.importDtos(List.of(
                new WorldCupPlayerDto("", "Spain", "GK", null),
                new WorldCupPlayerDto("Pedri", "", "MF", null),
                new WorldCupPlayerDto("UnknownPos", "Spain", "Goalie", null)
        ));

        assertThat(result.inserted()).isZero();
        assertThat(result.skipped()).isEqualTo(3);
        verify(repository, never()).save(any());
    }

    @Test
    void missingFileLogsAndReturnsEmptySummaryWithoutThrowing(@TempDir Path tmp) {
        var result = importer.importFrom(tmp.resolve("does-not-exist.json"));

        assertThat(result.total()).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    void readsFromRealJsonFile(@TempDir Path tmp) throws IOException {
        Path json = tmp.resolve("players.json");
        Files.writeString(json, """
                {
                  "competition": "Test Cup",
                  "total_players": 2,
                  "players": [
                    {"name":"Thibaut Courtois","national_team":"Belgium","position":"GK","image_url":"/players/belgium/thibaut-courtois.png"},
                    {"name":"Kevin De Bruyne","national_team":"Belgium","position":"MF","image_url":null}
                  ]
                }
                """);

        var result = importer.importFrom(json);

        assertThat(result.inserted()).isEqualTo(2);
        assertThat(store).hasSize(2);
        assertThat(store.get(key("Kevin De Bruyne", "Belgium")).getImageRef()).isNull();
    }

    private static String key(String name, String team) {
        return name + "|" + team;
    }
}
