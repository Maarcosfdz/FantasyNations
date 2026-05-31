package com.fantasynations.scoring.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public record ScoreBreakdown(int total, Map<String, Integer> byCategory) {
    public static ScoreBreakdown zero() {
        return new ScoreBreakdown(0, new LinkedHashMap<>());
    }
}
