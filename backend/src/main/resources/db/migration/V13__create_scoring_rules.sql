CREATE TABLE scoring_rules (
    code         VARCHAR(64) PRIMARY KEY,
    value        INT          NOT NULL,
    category     VARCHAR(16)  NOT NULL,
    position     VARCHAR(8),
    threshold    INT,
    bucket_size  INT,
    event_scope  VARCHAR(24)  NOT NULL DEFAULT 'NORMAL_OR_EXTRA_TIME',
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    description  VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_sr_category
        CHECK (category IN ('BASE','GK','CLEAN_SHEET','CONCEDED','SHOOTOUT','OPTIONAL')),
    CONSTRAINT chk_sr_position
        CHECK (position IS NULL OR position IN ('GK','DEF','MID','FWD','ANY')),
    CONSTRAINT chk_sr_event_scope
        CHECK (event_scope IN ('NORMAL_OR_EXTRA_TIME','SHOOTOUT','ANY'))
);

CREATE INDEX idx_scoring_rules_category ON scoring_rules(category);

-- BASE: minutes
INSERT INTO scoring_rules (code, value, category, position, threshold, event_scope, description) VALUES
    ('MINUTES_UNDER_60', 1, 'BASE', 'ANY', 1,  'ANY', 'Played 1-59 minutes'),
    ('MINUTES_60_PLUS',  2, 'BASE', 'ANY', 60, 'ANY', 'Played 60+ minutes');

-- BASE: goals per position
INSERT INTO scoring_rules (code, value, category, position, event_scope, description) VALUES
    ('GOAL_GK',  6, 'BASE', 'GK',  'NORMAL_OR_EXTRA_TIME', 'Goal scored by goalkeeper'),
    ('GOAL_DEF', 5, 'BASE', 'DEF', 'NORMAL_OR_EXTRA_TIME', 'Goal scored by defender'),
    ('GOAL_MID', 4, 'BASE', 'MID', 'NORMAL_OR_EXTRA_TIME', 'Goal scored by midfielder'),
    ('GOAL_FWD', 3, 'BASE', 'FWD', 'NORMAL_OR_EXTRA_TIME', 'Goal scored by forward');

-- BASE: misc events
INSERT INTO scoring_rules (code, value, category, position, event_scope, description) VALUES
    ('PENALTY_GOAL',       3,  'BASE', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Penalty goal in normal/extra time'),
    ('ASSIST',             3,  'BASE', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Assist'),
    ('BIG_CHANCE_CREATED', 1,  'OPTIONAL', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Big/clear chance created'),
    ('PENALTY_WON',        2,  'BASE', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Penalty won'),
    ('PENALTY_CONCEDED',  -2,  'BASE', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Penalty conceded'),
    ('PENALTY_MISSED',    -2,  'BASE', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Penalty missed in normal/extra time'),
    ('YELLOW_CARD',       -1,  'BASE', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Yellow card'),
    ('DOUBLE_YELLOW',     -3,  'BASE', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Second-yellow red'),
    ('DIRECT_RED',        -6,  'BASE', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Direct red card'),
    ('OWN_GOAL',          -2,  'BASE', 'ANY', 'NORMAL_OR_EXTRA_TIME', 'Own goal');

-- GK: saves and penalty saves
INSERT INTO scoring_rules (code, value, category, position, bucket_size, event_scope, description) VALUES
    ('SAVE_BUCKET',       1, 'GK', 'GK', 2, 'NORMAL_OR_EXTRA_TIME', '+1 per every 2 saves'),
    ('SAVE_BUCKET_BONUS', 1, 'GK', 'GK', 4, 'NORMAL_OR_EXTRA_TIME', '+1 bonus per every 4 saves');

INSERT INTO scoring_rules (code, value, category, position, event_scope, description) VALUES
    ('PENALTY_SAVED', 5, 'GK', 'GK', 'NORMAL_OR_EXTRA_TIME', 'Penalty saved by GK in normal/extra time');

-- CLEAN_SHEET: requires 60+ minutes
INSERT INTO scoring_rules (code, value, category, position, threshold, event_scope, description) VALUES
    ('CLEAN_SHEET_GK',  4, 'CLEAN_SHEET', 'GK',  60, 'ANY', 'Clean sheet GK (60+ min)'),
    ('CLEAN_SHEET_DEF', 3, 'CLEAN_SHEET', 'DEF', 60, 'ANY', 'Clean sheet DEF (60+ min)'),
    ('CLEAN_SHEET_MID', 2, 'CLEAN_SHEET', 'MID', 60, 'ANY', 'Clean sheet MID (60+ min)'),
    ('CLEAN_SHEET_FWD', 1, 'CLEAN_SHEET', 'FWD', 60, 'ANY', 'Clean sheet FWD (60+ min)');

-- CONCEDED: pairs of 2 while on pitch
INSERT INTO scoring_rules (code, value, category, position, bucket_size, event_scope, description) VALUES
    ('CONCEDED_GK',  -2, 'CONCEDED', 'GK',  2, 'NORMAL_OR_EXTRA_TIME', '-2 per 2 goals conceded on pitch (GK)'),
    ('CONCEDED_DEF', -2, 'CONCEDED', 'DEF', 2, 'NORMAL_OR_EXTRA_TIME', '-2 per 2 goals conceded on pitch (DEF)'),
    ('CONCEDED_MID', -1, 'CONCEDED', 'MID', 2, 'NORMAL_OR_EXTRA_TIME', '-1 per 2 goals conceded on pitch (MID)'),
    ('CONCEDED_FWD', -1, 'CONCEDED', 'FWD', 2, 'NORMAL_OR_EXTRA_TIME', '-1 per 2 goals conceded on pitch (FWD)');

-- SHOOTOUT
INSERT INTO scoring_rules (code, value, category, position, event_scope, description) VALUES
    ('SHOOTOUT_GOAL',  1, 'SHOOTOUT', 'ANY', 'SHOOTOUT', 'Penalty shootout goal'),
    ('SHOOTOUT_MISS', -1, 'SHOOTOUT', 'ANY', 'SHOOTOUT', 'Penalty shootout miss'),
    ('SHOOTOUT_SAVE',  3, 'SHOOTOUT', 'GK',  'SHOOTOUT', 'Penalty shootout save by GK');

-- OPTIONAL bonuses
INSERT INTO scoring_rules (code, value, category, position, bucket_size, event_scope, enabled, description) VALUES
    ('SHOTS_ON_TARGET',           1, 'OPTIONAL', 'ANY', 2, 'NORMAL_OR_EXTRA_TIME', FALSE, '+1 per 2 shots on target'),
    ('SUCCESSFUL_DRIBBLES',       1, 'OPTIONAL', 'ANY', 2, 'NORMAL_OR_EXTRA_TIME', FALSE, '+1 per 2 successful dribbles'),
    ('KEY_PASSES',                1, 'OPTIONAL', 'ANY', 2, 'NORMAL_OR_EXTRA_TIME', FALSE, '+1 per 2 key passes'),
    ('DUELS_INTERCEPTIONS',       1, 'OPTIONAL', 'ANY', 5, 'NORMAL_OR_EXTRA_TIME', FALSE, '+1 per 5 duels won + interceptions'),
    ('CLEARANCES',                1, 'OPTIONAL', 'ANY', 3, 'NORMAL_OR_EXTRA_TIME', FALSE, '+1 per 3 clearances');

INSERT INTO scoring_rules (code, value, category, position, event_scope, enabled, description) VALUES
    ('BIG_CHANCE_MISSED',  -1, 'OPTIONAL', 'ANY', 'NORMAL_OR_EXTRA_TIME', FALSE, 'Big chance missed'),
    ('ERROR_LEADING_GOAL', -2, 'OPTIONAL', 'ANY', 'NORMAL_OR_EXTRA_TIME', FALSE, 'Error leading to goal');
