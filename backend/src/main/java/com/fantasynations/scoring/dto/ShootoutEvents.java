package com.fantasynations.scoring.dto;

public record ShootoutEvents(int goals, int misses, int savesByGk) {
    public static ShootoutEvents empty() { return new ShootoutEvents(0, 0, 0); }
}
