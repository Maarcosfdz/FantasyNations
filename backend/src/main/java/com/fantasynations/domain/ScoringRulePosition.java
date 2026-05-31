package com.fantasynations.domain;

public enum ScoringRulePosition {
    GK, DEF, MID, FWD, ANY;

    public static ScoringRulePosition of(PlayerPosition position) {
        return switch (position) {
            case GK -> GK;
            case DEF -> DEF;
            case MID -> MID;
            case FWD -> FWD;
        };
    }
}
