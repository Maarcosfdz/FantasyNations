CREATE TABLE squad_players (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    squad_id        UUID NOT NULL REFERENCES squads(id) ON DELETE CASCADE,
    player_id       UUID NOT NULL REFERENCES players(id),
    release_clause  DECIMAL(12,2) NOT NULL,
    protected_until TIMESTAMP,
    acquired_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (squad_id, player_id)
);

CREATE INDEX idx_squad_players_squad ON squad_players(squad_id);
CREATE INDEX idx_squad_players_player ON squad_players(player_id);
