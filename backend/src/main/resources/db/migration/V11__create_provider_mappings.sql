CREATE TABLE provider_mappings (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_name      VARCHAR(64)  NOT NULL,
    entity_type        VARCHAR(32)  NOT NULL,
    internal_entity_id UUID         NOT NULL,
    external_entity_id VARCHAR(255) NOT NULL,
    source_url         VARCHAR(512),
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (provider_name, entity_type, internal_entity_id)
);

CREATE INDEX idx_provider_mappings_external ON provider_mappings(provider_name, entity_type, external_entity_id);
