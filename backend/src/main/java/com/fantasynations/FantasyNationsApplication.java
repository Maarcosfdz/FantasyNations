package com.fantasynations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FantasyNationsApplication {

    public static void main(String[] args) {
        SpringApplication.run(FantasyNationsApplication.class, args);
    }
}
