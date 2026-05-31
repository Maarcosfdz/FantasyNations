CREATE TABLE leagues (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(128) NOT NULL,
    owner_id    UUID NOT NULL REFERENCES users(id),
    invite_code VARCHAR(16)  NOT NULL UNIQUE,
    rules       TEXT         NOT NULL DEFAULT '{}',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_leagues_owner ON leagues(owner_id);
CREATE INDEX idx_leagues_invite_code ON leagues(invite_code);
