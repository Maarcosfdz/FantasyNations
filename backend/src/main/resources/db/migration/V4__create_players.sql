CREATE TABLE players (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(128) NOT NULL,
    national_team  VARCHAR(64)  NOT NULL,
    position       VARCHAR(8)   NOT NULL,
    base_value     DECIMAL(12,2) NOT NULL DEFAULT 1000000,
    current_value  DECIMAL(12,2) NOT NULL DEFAULT 1000000,
    image_ref      VARCHAR(255),
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_players_national_team ON players(national_team);
CREATE INDEX idx_players_position ON players(position);
CREATE INDEX idx_players_active ON players(active);
