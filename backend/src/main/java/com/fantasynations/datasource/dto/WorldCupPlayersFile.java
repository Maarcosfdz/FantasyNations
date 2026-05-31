package com.fantasynations.datasource.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldCupPlayersFile(
        String competition,
        String source,
        Integer total_players,
        List<WorldCupPlayerDto> players
) {}
