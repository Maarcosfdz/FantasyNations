CREATE TABLE activity_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id   UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id     UUID REFERENCES users(id),
    event_type  VARCHAR(32) NOT NULL,
    payload     TEXT NOT NULL DEFAULT '{}',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_activity_log_league ON activity_log(league_id);
CREATE INDEX idx_activity_log_created_at ON activity_log(created_at);
