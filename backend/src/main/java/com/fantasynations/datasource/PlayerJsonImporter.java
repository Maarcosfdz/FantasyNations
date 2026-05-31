package com.fantasynations.datasource;

import com.fantasynations.datasource.dto.WorldCupPlayerDto;
import com.fantasynations.datasource.dto.WorldCupPlayersFile;
import com.fantasynations.domain.PlayerPosition;
import com.fantasynations.domain.AvailabilityStatus;
import com.fantasynations.entity.PlayerEntity;
import com.fantasynations.marketvalue.InitialMarketValueService;
import com.fantasynations.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Imports World Cup players from a JSON file generated outside this repo.
 *
 * Idempotent: re-running the importer upserts by (name, national_team) and never
 * creates duplicates. Players with a null {@code image_url} are still inserted -
 * the frontend resolver renders the fallback for them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerJsonImporter {

    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;
    private final InitialMarketValueService initialMarketValueService;

    public record ImportSummary(int inserted, int updated, int skipped) {
        public int total() { return inserted + updated + skipped; }
    }

    /**
     * Reads players from {@code jsonPath} and upserts them.
     *
     * @return summary of inserted / updated / skipped rows; an empty summary
     *         is returned (and a clear error is logged) when the file is missing
     *         or unreadable - the application keeps starting.
     */
    @Transactional
    public ImportSummary importFrom(Path jsonPath) {
        if (jsonPath == null) {
            log.error("Player import skipped: app.players.json-path is not configured.");
            return new ImportSummary(0, 0, 0);
        }
        if (!Files.exists(jsonPath)) {
            log.error(
                    "Player import skipped: file not found at '{}'. " +
                    "Mount or copy fant/players.json to this path, or set app.players.json-path. " +
                    "App will start without importing players.",
                    jsonPath.toAbsolutePath()
            );
            return new ImportSummary(0, 0, 0);
        }
        if (!Files.isReadable(jsonPath)) {
            log.error("Player import skipped: file '{}' is not readable.", jsonPath.toAbsolutePath());
            return new ImportSummary(0, 0, 0);
        }

        final WorldCupPlayersFile file;
        try {
            file = objectMapper.readValue(jsonPath.toFile(), WorldCupPlayersFile.class);
        } catch (IOException e) {
            log.error("Player import failed: could not parse '{}': {}", jsonPath, e.getMessage());
            return new ImportSummary(0, 0, 0);
        }

        return importDtos(file == null ? List.of() : file.players());
    }

    @Transactional
    public ImportSummary importDtos(List<WorldCupPlayerDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            log.warn("Player import: no players in payload.");
            return new ImportSummary(0, 0, 0);
        }

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        // Reset the per-import fallback counters so importance assignments
        // are deterministic and based on import order grouped by team.
        initialMarketValueService.beginImportRun();

        for (WorldCupPlayerDto dto : dtos) {
            if (dto == null || isBlank(dto.name()) || isBlank(dto.national_team())) {
                log.warn("Skipping player with missing name or national_team: {}", dto);
                skipped++;
                continue;
            }
            Optional<PlayerPosition> position = PlayerPositionMapper.map(dto.position());
            if (position.isEmpty()) {
                log.warn("Skipping {} ({}): unknown position '{}'.",
                        dto.name(), dto.national_team(), dto.position());
                skipped++;
                continue;
            }

            Optional<PlayerEntity> existing = playerRepository
                    .findByNameAndNationalTeam(dto.name(), dto.national_team());

            if (existing.isPresent()) {
                PlayerEntity player = existing.get();
                boolean changed = false;
                if (player.getPosition() != position.get()) {
                    player.setPosition(position.get());
                    changed = true;
                }
                if (!Objects.equals(player.getImageRef(), dto.image_url())) {
                    player.setImageRef(dto.image_url());
                    changed = true;
                }
                if (!player.isActive()) {
                    player.setActive(true);
                    changed = true;
                }
                if (changed) {
                    playerRepository.save(player);
                    updated++;
                } else {
                    skipped++;
                }
            } else {
                PlayerEntity player = PlayerEntity.builder()
                        .name(dto.name())
                        .nationalTeam(dto.national_team())
                        .position(position.get())
                        // Placeholders; InitialMarketValueService overwrites these.
                        .baseValue(BigDecimal.ZERO)
                        .currentValue(BigDecimal.ZERO)
                        .initialMarketValue(BigDecimal.ZERO)
                        .marketValue(BigDecimal.ZERO)
                        .availabilityStatus(AvailabilityStatus.AVAILABLE)
                        .imageRef(dto.image_url())
                        .active(true)
                        .build();
                initialMarketValueService.applyForNewPlayer(player);
                inserted++;
            }
        }

        log.info("Player import complete: inserted={}, updated={}, skipped={} (total received={}).",
                inserted, updated, skipped, dtos.size());
        return new ImportSummary(inserted, updated, skipped);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
