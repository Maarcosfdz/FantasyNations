-- Slice A of the market value system: add the new player-level fields and
-- widen money columns to whole-unit DECIMAL(18,0). Existing rows keep their
-- old base_value/current_value; market_value is backfilled from current_value
-- (rounded down to the nearest unit) so the importer can run idempotently.

ALTER TABLE players
    ALTER COLUMN base_value    TYPE DECIMAL(18,0) USING base_value::DECIMAL(18,0),
    ALTER COLUMN current_value TYPE DECIMAL(18,0) USING current_value::DECIMAL(18,0);

ALTER TABLE players
    ADD COLUMN initial_market_value    DECIMAL(18,0),
    ADD COLUMN market_value            DECIMAL(18,0),
    ADD COLUMN importance              VARCHAR(24),
    ADD COLUMN league_reputation       VARCHAR(16),
    ADD COLUMN availability_status     VARCHAR(24) NOT NULL DEFAULT 'AVAILABLE';

UPDATE players SET market_value = current_value WHERE market_value IS NULL;
UPDATE players SET initial_market_value = current_value WHERE initial_market_value IS NULL;

ALTER TABLE players
    ALTER COLUMN initial_market_value SET NOT NULL,
    ALTER COLUMN market_value SET NOT NULL;

ALTER TABLE players
    ADD CONSTRAINT chk_players_importance
        CHECK (importance IS NULL OR importance IN
            ('GLOBAL_SUPERSTAR','STAR','STARTER','ROTATION','BENCH'));

ALTER TABLE players
    ADD CONSTRAINT chk_players_league_reputation
        CHECK (league_reputation IS NULL OR league_reputation IN
            ('ELITE','STRONG','MEDIUM','LOW'));

ALTER TABLE players
    ADD CONSTRAINT chk_players_availability
        CHECK (availability_status IN
            ('AVAILABLE','DOUBTFUL','INJURED','SUSPENDED','OUT_OF_TOURNAMENT'));

CREATE INDEX idx_players_importance ON players(importance);
CREATE INDEX idx_players_availability ON players(availability_status);
