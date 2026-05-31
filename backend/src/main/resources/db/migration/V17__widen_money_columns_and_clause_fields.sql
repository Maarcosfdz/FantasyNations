-- Widen all remaining money columns to whole-unit DECIMAL(18,0) so they can
-- safely hold values up to the new 200M starting budget and beyond, and add
-- the per-ownership fixed release-clause fields needed by ReleaseClauseService.

ALTER TABLE squad_players
    ALTER COLUMN release_clause TYPE DECIMAL(18,0) USING release_clause::DECIMAL(18,0);

ALTER TABLE squad_players
    ADD COLUMN fixed_release_clause_value     DECIMAL(18,0),
    ADD COLUMN release_clause_manually_raised BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE league_members
    ALTER COLUMN money TYPE DECIMAL(18,0) USING money::DECIMAL(18,0);

ALTER TABLE market_players
    ALTER COLUMN price TYPE DECIMAL(18,0) USING price::DECIMAL(18,0);
