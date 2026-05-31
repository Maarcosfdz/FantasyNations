package com.fantasynations.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinLeagueRequestDto(
        @NotBlank String inviteCode
) {}
