CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL UNIQUE,
    nickname    VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(255),
    avatar_url  VARCHAR(512),
    provider    VARCHAR(32)  NOT NULL DEFAULT 'local',
    provider_id VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_provider_id ON users(provider, provider_id);
