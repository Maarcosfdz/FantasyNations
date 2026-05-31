package com.fantasynations.datasource;

import com.fantasynations.datasource.normalizer.PlayerNormalizer;
import com.fantasynations.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Legacy seeder for the fictional mock players. Disabled by default - the real
 * World Cup players come from {@link PlayerJsonImporter}. Re-enable for tests
 * or local experiments by setting {@code app.players.mock-seed-on-startup=true}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class DataSourceInitializer implements ApplicationRunner {

    private final SportsDataSource sportsDataSource;
    private final PlayerRepository playerRepository;
    private final PlayerNormalizer playerNormalizer;

    @Value("${app.players.mock-seed-on-startup:false}")
    private boolean mockSeedOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!mockSeedOnStartup) {
            log.debug("Mock player seeder disabled (app.players.mock-seed-on-startup=false).");
            return;
        }
        if (playerRepository.count() == 0) {
            log.info("Seeding fictional mock players from data source...");
            var players = sportsDataSource.getPlayers().stream()
                    .map(playerNormalizer::normalize)
                    .toList();
            playerRepository.saveAll(players);
            log.info("Seeded {} fictional players.", players.size());
        }
    }
}
