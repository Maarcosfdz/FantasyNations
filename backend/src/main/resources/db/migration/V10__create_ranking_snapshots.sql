CREATE TABLE ranking_snapshots (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    league_id    UUID NOT NULL REFERENCES leagues(id) ON DELETE CASCADE,
    user_id      UUID NOT NULL REFERENCES users(id),
    total_points INT  NOT NULL DEFAULT 0,
    rank         INT  NOT NULL DEFAULT 0,
    snapshot_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ranking_snapshots_league ON ranking_snapshots(league_id);
CREATE INDEX idx_ranking_snapshots_snapshot_at ON ranking_snapshots(snapshot_at);
