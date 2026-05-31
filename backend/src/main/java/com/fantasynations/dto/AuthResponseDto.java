package com.fantasynations.dto;

import java.util.UUID;

public record AuthResponseDto(
        String token,
        UUID userId,
        String email,
        String nickname,
        String avatarUrl
) {}
