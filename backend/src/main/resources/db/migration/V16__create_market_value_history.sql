CREATE TABLE market_value_history (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id        UUID         NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    old_value        DECIMAL(18,0) NOT NULL,
    new_value        DECIMAL(18,0) NOT NULL,
    delta            DECIMAL(18,0) NOT NULL,
    delta_percent    DECIMAL(6,2),
    momentum_score   INT,
    reason           VARCHAR(32)  NOT NULL,
    matchday_id      UUID,
    market_cycle_id  UUID,
    breakdown_json   TEXT,
    calculated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_mvh_reason CHECK (reason IN
        ('INITIAL_VALUE','HIGH_PERFORMANCE','LOW_PERFORMANCE','DID_NOT_PLAY',
         'INJURED','SUSPENDED','TEAM_ELIMINATED','MARKET_DEMAND','STABLE','MANUAL_OVERRIDE'))
);

CREATE INDEX idx_mvh_player        ON market_value_history(player_id, calculated_at DESC);
CREATE INDEX idx_mvh_reason        ON market_value_history(reason);
CREATE INDEX idx_mvh_matchday      ON market_value_history(matchday_id);
