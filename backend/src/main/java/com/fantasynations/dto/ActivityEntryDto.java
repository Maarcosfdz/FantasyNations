package com.fantasynations.dto;

import com.fantasynations.domain.ActivityEventType;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ActivityEntryDto(
        UUID id,
        ActivityEventType eventType,
        String userNickname,
        Map<String, Object> payload,
        LocalDateTime createdAt
) {}
