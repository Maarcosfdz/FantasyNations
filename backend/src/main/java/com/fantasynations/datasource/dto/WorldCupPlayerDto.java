package com.fantasynations.datasource.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorldCupPlayerDto(
        String name,
        String national_team,
        String position,
        String image_url
) {}
