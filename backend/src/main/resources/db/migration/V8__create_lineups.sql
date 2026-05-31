CREATE TABLE lineups (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id   UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (league_id, user_id)
);

CREATE TABLE lineup_players (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lineup_id       UUID NOT NULL REFERENCES lineups(id) ON DELETE CASCADE,
    player_id       UUID NOT NULL REFERENCES players(id),
    position_slot   VARCHAR(8) NOT NULL,
    UNIQUE (lineup_id, player_id)
);

CREATE INDEX idx_lineups_league ON lineups(league_id);
CREATE INDEX idx_lineups_user ON lineups(user_id);
CREATE INDEX idx_lineup_players_lineup ON lineup_players(lineup_id);
