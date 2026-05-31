CREATE TABLE league_members (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id   UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    role        VARCHAR(16)   NOT NULL DEFAULT 'MEMBER',
    money       DECIMAL(15,2) NOT NULL DEFAULT 0,
    joined_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (league_id, user_id)
);

CREATE INDEX idx_league_members_league ON league_members(league_id);
CREATE INDEX idx_league_members_user ON league_members(user_id);
