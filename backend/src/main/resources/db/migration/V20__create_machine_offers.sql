CREATE TABLE machine_offers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id        UUID         NOT NULL REFERENCES market_cycles(id) ON DELETE CASCADE,
    league_id       UUID         NOT NULL REFERENCES leagues(id)        ON DELETE CASCADE,
    squad_player_id UUID         NOT NULL REFERENCES squad_players(id)  ON DELETE CASCADE,
    seller_user_id  UUID         NOT NULL REFERENCES users(id)          ON DELETE CASCADE,
    amount          DECIMAL(18,0) NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    expires_at      TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    accepted_at     TIMESTAMP,
    CONSTRAINT chk_machine_offer_status
        CHECK (status IN ('PENDING','ACCEPTED','EXPIRED'))
);

CREATE INDEX idx_machine_offers_league_cycle ON machine_offers(league_id, cycle_id);
CREATE INDEX idx_machine_offers_squad_player ON machine_offers(squad_player_id);
CREATE INDEX idx_machine_offers_status       ON machine_offers(status);
