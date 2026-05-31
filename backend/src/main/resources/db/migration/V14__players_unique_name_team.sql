-- World Cup players are uniquely identified by (name, national_team).
-- The importer relies on this constraint for idempotent upserts.
ALTER TABLE players
    ADD CONSTRAINT uq_players_name_team UNIQUE (name, national_team);
