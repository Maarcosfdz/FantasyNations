CREATE TABLE squads (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id   UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (league_id, user_id)
);

CREATE INDEX idx_squads_league ON squads(league_id);
CREATE INDEX idx_squads_user ON squads(user_id);
