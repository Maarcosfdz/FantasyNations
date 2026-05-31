package com.fantasynations.datasource.dto;

public record ExternalPlayerDto(
        String externalId,
        String name,
        String nationalTeam,
        String position,
        double baseValue,
        String imageRef
) {}
