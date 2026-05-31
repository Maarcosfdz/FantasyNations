package com.fantasynations.datasource.dto;

public record ExternalTeamDto(
        String externalId,
        String name,
        String countryCode,
        String logoRef
) {}
