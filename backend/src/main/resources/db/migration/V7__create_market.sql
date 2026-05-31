CREATE TABLE market_players (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id       UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    player_id       UUID NOT NULL REFERENCES players(id),
    price           DECIMAL(12,2) NOT NULL,
    available_until TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (league_id, player_id)
);

CREATE INDEX idx_market_players_league ON market_players(league_id);
CREATE INDEX idx_market_players_available ON market_players(available_until);
