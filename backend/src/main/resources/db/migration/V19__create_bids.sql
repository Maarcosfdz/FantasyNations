CREATE TABLE bids (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id          UUID         NOT NULL REFERENCES market_cycles(id) ON DELETE CASCADE,
    market_player_id  UUID         NOT NULL REFERENCES market_players(id) ON DELETE CASCADE,
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount            DECIMAL(18,0) NOT NULL,
    status            VARCHAR(24)  NOT NULL DEFAULT 'SUBMITTED',
    submitted_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    resolved_at       TIMESTAMP,
    CONSTRAINT chk_bid_status
        CHECK (status IN ('SUBMITTED','WON','LOST','REJECTED_NO_FUNDS')),
    -- One bid per user per listing. Re-submission overwrites via UPDATE.
    UNIQUE (market_player_id, user_id)
);

CREATE INDEX idx_bids_cycle  ON bids(cycle_id);
CREATE INDEX idx_bids_user   ON bids(user_id);
CREATE INDEX idx_bids_listing_amount ON bids(market_player_id, amount DESC, submitted_at ASC);
