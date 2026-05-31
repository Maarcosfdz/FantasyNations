-- Locked lineup snapshot + persisted matchday scores. The snapshot is the
-- only lineup used to calculate that matchday. Re-aggregation reuses it.

ALTER TABLE lineups
    ADD COLUMN frozen_at TIMESTAMP,
    ADD COLUMN frozen_for_matchday_id UUID;

CREATE TABLE locked_lineup_players (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lineup_id       UUID NOT NULL REFERENCES lineups(id) ON DELETE CASCADE,
    matchday_id     UUID NOT NULL REFERENCES matchdays(id) ON DELETE CASCADE,
    league_id       UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    player_id       UUID NOT NULL REFERENCES players(id),
    position_slot   VARCHAR(8) NOT NULL,
    locked_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (lineup_id, matchday_id, position_slot)
);

CREATE INDEX idx_llp_matchday    ON locked_lineup_players(matchday_id);
CREATE INDEX idx_llp_user_league ON locked_lineup_players(user_id, league_id);

CREATE TABLE matchday_scores (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id       UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users(id),
    matchday_id     UUID NOT NULL REFERENCES matchdays(id) ON DELETE CASCADE,
    total_points    INT NOT NULL,
    reason          VARCHAR(32) NOT NULL,
    aggregated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_md_score_reason CHECK (reason IN ('OK','INCOMPLETE_LINEUP','NEGATIVE_BALANCE')),
    UNIQUE (league_id, user_id, matchday_id)
);

CREATE INDEX idx_md_scores_matchday ON matchday_scores(matchday_id);

CREATE TABLE player_matchday_scores (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matchday_score_id    UUID NOT NULL REFERENCES matchday_scores(id) ON DELETE CASCADE,
    player_id            UUID NOT NULL REFERENCES players(id),
    position_slot        VARCHAR(8) NOT NULL,
    points               INT NOT NULL,
    breakdown_json       TEXT,
    UNIQUE (matchday_score_id, player_id)
);

CREATE INDEX idx_pmds_matchday_score ON player_matchday_scores(matchday_score_id);
