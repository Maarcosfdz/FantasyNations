package com.fantasynations.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Triggers the World Cup player import on application startup when enabled.
 * The JSON path is configurable via {@code app.players.json-path}; nothing is
 * imported if the file does not exist (the importer logs a clear error).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlayerImportRunner implements ApplicationRunner {

    private final PlayerJsonImporter importer;

    @Value("${app.players.import-on-startup:true}")
    private boolean importOnStartup;

    @Value("${app.players.json-path:../fant/players.json}")
    private String jsonPath;

    @Override
    public void run(ApplicationArguments args) {
        if (!importOnStartup) {
            log.info("Player import on startup is disabled (app.players.import-on-startup=false).");
            return;
        }
        Path path = Paths.get(jsonPath);
        log.info("Importing World Cup players from '{}'...", path.toAbsolutePath());
        importer.importFrom(path);
    }
}
