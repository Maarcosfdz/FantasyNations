CREATE TABLE matchdays (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    number      INT          NOT NULL UNIQUE,
    phase       VARCHAR(16)  NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'SCHEDULED',
    lock_at     TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_matchday_number CHECK (number BETWEEN 1 AND 7),
    CONSTRAINT chk_matchday_phase  CHECK (phase IN ('GROUP','R16','QF','SF','FINAL')),
    CONSTRAINT chk_matchday_status CHECK (status IN ('SCHEDULED','LOCKED','FINISHED'))
);

CREATE INDEX idx_matchdays_phase ON matchdays(phase);

CREATE TABLE real_matches (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    matchday_id  UUID         NOT NULL REFERENCES matchdays(id) ON DELETE CASCADE,
    kickoff      TIMESTAMP    NOT NULL,
    home_team    VARCHAR(64)  NOT NULL,
    away_team    VARCHAR(64)  NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT 'SCHEDULED',
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_real_match_status
        CHECK (status IN ('SCHEDULED','IN_PROGRESS','FINISHED','CANCELLED','POSTPONED'))
);

CREATE INDEX idx_real_matches_matchday ON real_matches(matchday_id);
CREATE INDEX idx_real_matches_kickoff  ON real_matches(kickoff);

INSERT INTO matchdays (number, phase) VALUES
    (1, 'GROUP'),
    (2, 'GROUP'),
    (3, 'GROUP'),
    (4, 'R16'),
    (5, 'QF'),
    (6, 'SF'),
    (7, 'FINAL');
