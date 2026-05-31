-- Per-player, per-real-match stats. One row per (player, real_match).
-- This table is the input to the scoring engine; rows are inserted by the
-- admin POST endpoint or any future provider plug-in.

CREATE TABLE player_match_stats (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id                       UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    real_match_id                   UUID NOT NULL REFERENCES real_matches(id) ON DELETE CASCADE,

    -- presence
    did_not_play                    BOOLEAN NOT NULL DEFAULT FALSE,
    minutes_played                  INT NOT NULL DEFAULT 0,
    on_pitch_goals_conceded         INT NOT NULL DEFAULT 0,
    team_clean_sheet                BOOLEAN NOT NULL DEFAULT FALSE,

    -- normal / extra time events
    goals                           INT NOT NULL DEFAULT 0,
    penalty_goals                   INT NOT NULL DEFAULT 0,
    assists                         INT NOT NULL DEFAULT 0,
    big_chances_created             INT NOT NULL DEFAULT 0,
    penalties_won                   INT NOT NULL DEFAULT 0,
    penalties_conceded              INT NOT NULL DEFAULT 0,
    penalties_missed                INT NOT NULL DEFAULT 0,
    penalties_saved_by_gk           INT NOT NULL DEFAULT 0,
    saves                           INT NOT NULL DEFAULT 0,
    yellow_cards                    INT NOT NULL DEFAULT 0,
    double_yellows                  INT NOT NULL DEFAULT 0,
    direct_reds                     INT NOT NULL DEFAULT 0,
    own_goals                       INT NOT NULL DEFAULT 0,

    -- shootout events
    shootout_goals                  INT NOT NULL DEFAULT 0,
    shootout_misses                 INT NOT NULL DEFAULT 0,
    shootout_saves_by_gk            INT NOT NULL DEFAULT 0,

    -- optional stats
    shots_on_target                 INT NOT NULL DEFAULT 0,
    successful_dribbles             INT NOT NULL DEFAULT 0,
    key_passes                      INT NOT NULL DEFAULT 0,
    duels_won_plus_interceptions    INT NOT NULL DEFAULT 0,
    clearances                      INT NOT NULL DEFAULT 0,
    big_chances_missed              INT NOT NULL DEFAULT 0,
    error_leading_to_goal           INT NOT NULL DEFAULT 0,

    created_at                      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (player_id, real_match_id)
);

CREATE INDEX idx_pms_real_match ON player_match_stats(real_match_id);
CREATE INDEX idx_pms_player     ON player_match_stats(player_id);
