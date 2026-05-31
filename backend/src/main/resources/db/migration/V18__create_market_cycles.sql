CREATE TABLE market_cycles (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id     UUID         NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    cycle_number  INT          NOT NULL,
    opens_at      TIMESTAMP    NOT NULL,
    closes_at     TIMESTAMP    NOT NULL,
    resolved_at   TIMESTAMP,
    status        VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_market_cycle_status
        CHECK (status IN ('OPEN','RESOLVING','CLOSED')),
    UNIQUE (league_id, cycle_number)
);

CREATE INDEX idx_market_cycles_league_status ON market_cycles(league_id, status);
CREATE INDEX idx_market_cycles_closes_at     ON market_cycles(closes_at);

ALTER TABLE market_players
    ADD COLUMN cycle_id UUID REFERENCES market_cycles(id) ON DELETE CASCADE;

CREATE INDEX idx_market_players_cycle ON market_players(cycle_id);
