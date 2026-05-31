package com.fantasynations.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * Single RandomGenerator bean used by services that need a random source
 * (initial squad assignment, etc.). Centralising it makes the random source
 * trivially swappable in tests with a seeded generator for determinism.
 */
@Configuration
public class RandomConfig {

    @Bean
    public RandomGenerator randomGenerator() {
        return ThreadLocalRandom.current();
    }
}
