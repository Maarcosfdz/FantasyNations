# World Cup Scoring & Matchday Rules — Design Specification

**Status:** SPECIFICATION ONLY — no code is implemented in this slice. This document defines what must be built later.

**Date:** 2026-05-18
**Scope:** Backend scoring engine, matchday model, and supporting services for a World Cup-format FantasyNations tournament.

---

## 1. Purpose

Define a complete, configurable, deterministic scoring and matchday system for a World Cup edition of FantasyNations. Marca Fantasy is inspiration only; FantasyNations rules are explicit and self-contained.

The scoring engine must be:

- Deterministic — same input always produces the same output.
- Configurable — **scoring point values, thresholds and bucket sizes MUST NOT be hardcoded anywhere in service code.** Every numeric value used by the engine (point values, the `60`-minute threshold, the `2`/`4`/`5`/`3` save and stat bucket sizes, etc.) must come from the `scoring_rules` table or from typed configuration objects bound at startup.
- Testable — pure functions over DTOs, covered by unit tests.
- Free of external calls.
- **Captain multipliers and any other point-multiplier mechanics are explicitly disabled and out of scope** for this specification. They may be added in a later, separate spec.

---

## 2. World Cup Matchdays

A matchday groups all the real matches that contribute to a single fantasy scoring round.

| Matchday | Phase                          | Real matches included                        |
|---------:|--------------------------------|----------------------------------------------|
| 1        | Group stage                    | First group match of every national team    |
| 2        | Group stage                    | Second group match of every national team   |
| 3        | Group stage                    | Third group match of every national team    |
| 4        | Knockout                       | Round of 16                                  |
| 5        | Knockout                       | Quarter-finals                               |
| 6        | Knockout                       | Semi-finals                                  |
| 7        | Knockout                       | Third-place match and final                  |

### 2.1 Group stage rules

- Every national team participates in matchdays 1, 2 and 3.
- Each matchday spans the calendar window during which that group-stage round is played.
- A player scores in the matchday corresponding to his national team's match in that round.

### 2.2 Knockout phase rules

- Matchdays 4–7 only include the teams that qualified for that round.
- A user's player whose team did NOT qualify for the round scores 0 for that matchday but remains in the squad.
- For matchday 7, ONLY players whose national team is involved in the final OR the third-place match can score. Every other player in the user's lineup scores 0 for matchday 7, even if his team played earlier rounds.

---

## 3. Lineup Lock

- Each matchday has a `lockAt` timestamp.
- `lockAt` is set to the kickoff time of the FIRST real match in that matchday.
- After `lockAt`, the saved lineup is frozen for that matchday.
- Only players present in the locked lineup contribute points.
- Edits to the lineup after `lockAt` do not affect the current matchday.

### 3.1 Complete vs. incomplete lineup

A **complete lineup** means:

- exactly 11 players assigned to the 11 starting slots; AND
- the lineup satisfies a valid formation (e.g. 1 GK + 10 outfielders in a recognised shape).

Formation validation may be driven by league settings (allowed formations list per league), but `MatchdayAggregationService` MUST reject any lineup that is not complete by both rules above.

- If the lineup at `lockAt` is incomplete (fewer than 11 players OR invalid formation), the user scores **0 points** for the entire matchday.
- No per-empty-slot penalty is applied. The −4 rule from other fantasy games is explicitly rejected.

### 3.2 Negative balance

- If the user's money balance is negative at `lockAt`, the user scores **0 points** for the entire matchday, regardless of lineup state.
- Negative-balance zeroing takes precedence over normal scoring but is recorded for transparency in the matchday breakdown.

---

## 4. World Cup Exceptions

- **Eliminated team:** a player whose national team is eliminated remains in the squad but cannot score in future matchdays.
- **Did not play:** a player who is in the matchday squad but does not appear receives 0 points.
- **Substitutes:** appearances off the bench count normally; minutes count from time of entry.
- **Extra time:** all event types (goals, assists, cards, saves, penalties, clean-sheet minutes) count exactly like normal time.
- **Penalty shootouts:** events count but with reduced value (see §8).
- **Suspended / postponed matches:** scoring remains pending until the match is played; the player scores 0 until then.
- **Cancelled and never played:** all players in that match receive 0 points permanently.

---

## 5. Base Scoring (normal time + extra time)

All values are defaults. Every value (point amounts, the 60-minute threshold, bucket sizes) MUST live in the `scoring_rules` table or in a bound typed configuration object; the service MUST NOT contain numeric literals for points, thresholds, or bucket sizes.

| Event                                                            | Points |
|------------------------------------------------------------------|-------:|
| Played < 60 minutes                                              |     +1 |
| Played ≥ 60 minutes                                              |     +2 |
| Goal — goalkeeper                                                |     +6 |
| Goal — defender                                                  |     +5 |
| Goal — midfielder                                                |     +4 |
| Goal — forward                                                   |     +3 |
| Penalty goal in normal/extra time (any position)                 |     +3 |
| Assist                                                           |     +3 |
| Big chance / clear chance created (optional, if data available)  |     +1 |
| Penalty won                                                      |     +2 |
| Penalty conceded                                                 |     −2 |
| Penalty missed in normal/extra time                              |     −2 |
| Penalty saved by GK in normal/extra time                         |     +5 |
| Yellow card                                                      |     −1 |
| Second-yellow red                                                |     −3 |
| Direct red card                                                  |     −6 |
| Own goal                                                         |     −2 |

A penalty goal scored in normal/extra time uses the penalty-goal value (+3) and NOT the position-based goal value. The rule engine must select one, never both.

---

## 6. Goalkeeper Scoring

Goalkeepers must be able to score from saves, not only clean sheets.

| Event                                              | Points       |
|----------------------------------------------------|-------------:|
| Every 2 saves                                      | +1           |
| Bonus per every 4 saves                            | +1 additional|
| Penalty saved in normal/extra time                 | +5           |
| Penalty saved in shootout                          | +3           |
| Clean sheet with ≥ 60 minutes played               | +4           |
| Goalkeeper goal                                    | +6           |
| Goalkeeper assist                                  | +3           |

### 6.1 Save-bonus reference

| Saves | From `saves/2` | Bonus (`saves/4`) | Total |
|------:|--------------:|------------------:|------:|
|     2 |             1 |                 0 |     1 |
|     4 |             2 |                 1 |     3 |
|     6 |             3 |                 1 |     4 |
|     8 |             4 |                 2 |     6 |

Both buckets use integer division.

---

## 7. Clean Sheet and Goals Conceded

### 7.1 Clean sheet

Player must have completed ≥ 60 minutes.

| Position    | Points |
|-------------|-------:|
| Goalkeeper  |     +4 |
| Defender    |     +3 |
| Midfielder  |     +2 |
| Forward     |     +1 |

If the team concedes AFTER the player was substituted off and the player had already completed 60 minutes, the clean sheet still counts for that player.

### 7.2 Goals conceded

Applied only for goals conceded while the player is on the pitch.

| Position group   | Points                                |
|------------------|---------------------------------------|
| GK / DEF         | −2 per every 2 goals conceded on pitch|
| MID / FWD        | −1 per every 2 goals conceded on pitch|

Integer division. Odd counts do not get a partial penalty.

---

## 8. Penalty Shootout Scoring

Shootout events are SEPARATE from normal/extra-time events and have lower value.

| Event                            | Points |
|----------------------------------|-------:|
| Shootout goal                    |     +1 |
| Shootout miss                    |     −1 |
| Shootout save by GK              |     +3 |

Rules:

- Shootout goals do NOT count as normal goals.
- Shootout saves do NOT count as normal penalty saves.
- Shootout events do NOT generate assists, penalty-won, or penalty-conceded points.
- Extra-time penalty events ARE normal events, not shootout events.

---

## 9. Optional Statistical Bonuses

All optional. Each rule MUST be individually toggleable via `scoring_rules.enabled`. Disabled rules contribute 0.

| Event                                        | Points              |
|----------------------------------------------|--------------------:|
| Shots on target                              | +1 per every 2      |
| Successful dribbles                          | +1 per every 2      |
| Key passes                                   | +1 per every 2      |
| Duels won + interceptions (combined)         | +1 per every 5      |
| Clearances                                   | +1 per every 3      |
| Big chances missed                           | −1 each             |
| Error leading to goal                        | −2 each             |

If the data source does not provide a stat, the rule remains disabled and is never applied.

---

## 10. Configurable Scoring Rules

### 10.1 Storage

A `scoring_rules` table will store every numeric value used by the engine.

| Column       | Type          | Notes                                                                                |
|--------------|---------------|--------------------------------------------------------------------------------------|
| code         | varchar PK    | e.g. `GOAL`, `CLEAN_SHEET`, `SAVE_BUCKET`, `SHOOTOUT_GOAL`                          |
| value        | int           | Signed point value awarded each time the bucket/threshold is met                     |
| category     | varchar/enum  | BASE, GK, CLEAN_SHEET, CONCEDED, SHOOTOUT, OPTIONAL                                  |
| position     | varchar/enum  | NULLABLE. GK / DEF / MID / FWD / ANY — restricts the rule to a position group        |
| threshold    | int           | NULLABLE. Minimum value of the underlying stat for the rule to apply (e.g. 60 mins)  |
| bucket_size  | int           | NULLABLE. When set, points = `value * floor(stat / bucket_size)` (e.g. 2 saves → +1) |
| event_scope  | varchar/enum  | NORMAL_OR_EXTRA_TIME / SHOOTOUT / ANY — keeps shootout events isolated from match events |
| enabled      | boolean       | False disables the rule                                                              |

Examples of how the columns combine:

- `GOAL` with `position=DEF`, `value=5`, `event_scope=NORMAL_OR_EXTRA_TIME`.
- `PENALTY_GOAL` with `position=ANY`, `value=3`, `event_scope=NORMAL_OR_EXTRA_TIME`.
- `SAVE_BUCKET` with `position=GK`, `value=1`, `bucket_size=2`, `event_scope=NORMAL_OR_EXTRA_TIME`.
- `SAVE_BUCKET_BONUS` with `position=GK`, `value=1`, `bucket_size=4`, `event_scope=NORMAL_OR_EXTRA_TIME`.
- `CLEAN_SHEET` with `position=DEF`, `value=3`, `threshold=60`.
- `SHOOTOUT_GOAL` with `position=ANY`, `value=1`, `event_scope=SHOOTOUT`.

The table will be created by a **future Flyway migration**. No JPA auto-DDL is used in production; the migration is the source of truth for the schema and the default rows.

### 10.2 Access

- `ScoringRulesRepository extends JpaRepository<ScoringRuleEntity, String>`
- `ScoringRulesProvider` loads all rules into an in-memory `Map<String,Integer>` on startup, exposes typed accessors (`pointsPerGoal(PlayerPosition)`, `cleanSheet(PlayerPosition)`, `saveEvery2()`, `shootoutGoal()`, …) and a `reload()` method.
- The service layer NEVER references numeric literals.

### 10.3 Seeding

Default rows are inserted by the same Flyway migration that creates the table. Defaults are the values in §5–§9 of this document. A code-side seeder is NOT used.

---

## 11. Score Calculation Service

### 11.1 DTOs (immutable records, `com.fantasynations.scoring.dto`)

`PerformanceStats` describes what happened on the pitch ONLY. Tournament context (eliminated, qualified for current round, team-in-matchday-7-final, etc.) lives in a separate `MatchdayEligibility` DTO. This keeps `PerformanceStats` reusable for any format and reserves tournament-shape rules to the aggregation layer.

- `PerformanceStats` (per-match performance)
  - `position`, `minutesPlayed`, `didNotPlay`
  - `onPitchGoalsConceded`, `teamCleanSheet`
  - `events` (MatchEvents), `shootout` (ShootoutEvents), `optional` (OptionalStats)
- `MatchdayEligibility` (per-player per-matchday tournament context)
  - `eliminated` — team eliminated before this matchday
  - `qualifiedForRound` — team participates in this matchday's round
  - `matchday7Slot` — NONE / FINAL / THIRD_PLACE (only relevant for matchday 7)
  - The aggregator returns 0 for a player when `eliminated` or `!qualifiedForRound`, or when on matchday 7 the player's `matchday7Slot == NONE`. `FantasyScoringService` itself is unaware of tournament context.
- `MatchEvents` — `goals`, `penaltyGoals`, `assists`, `bigChancesCreated`, `penaltiesWon`, `penaltiesConceded`, `penaltiesMissed`, `penaltiesSavedByGk`, `saves`, `yellowCards`, `doubleYellows`, `directReds`, `ownGoals`
- `ShootoutEvents` — `goals`, `misses`, `savesByGk`
- `OptionalStats` — `shotsOnTarget`, `successfulDribbles`, `keyPasses`, `duelsWonPlusInterceptions`, `clearances`, `bigChancesMissed`, `errorLeadingToGoal`
- `ScoreBreakdown` — `int total`, `Map<String,Integer> byCategory`

### 11.2 Behavior

`FantasyScoringService.calculate(PerformanceStats)` returns `ScoreBreakdown`.

Order of evaluation:

1. If `didNotPlay` → return `{total=0}`. (Tournament-context zeroing — `eliminated`, knockout non-qualification, matchday-7 slot — happens in the aggregator, not here.)
2. Minutes-played base points.
3. Goals (split: position goals vs. penalty goals).
4. Assists, big-chance-created.
5. Penalties won / conceded / missed / saved (normal/ET only).
6. Cards, own goals.
7. GK-only block: save buckets, GK clean sheet.
8. Outfield clean sheet (gated by ≥60 min).
9. Goals conceded (pairs of 2, only `onPitchGoalsConceded`).
10. Shootout block (independent from normal events).
11. Optional stats (each rule respects `enabled`).

All numeric values pulled from `ScoringRulesProvider`. No literals in this service.

### 11.3 Aggregation (separate service, future slice)

`MatchdayAggregationService.calculate(userId, leagueId, matchdayId)`:

- Reads locked lineup as of `matchday.lockAt`.
- If lineup is incomplete (fewer than 11 OR invalid formation) → returns 0.
- If user balance was negative at `lockAt` → returns 0.
- For each player in the locked lineup, builds a `MatchdayEligibility`:
  - if `eliminated` or `!qualifiedForRound` → contribute 0.
  - on matchday 7, contribute 0 if the player's team is not in the final or the third-place match.
- Otherwise: sums `FantasyScoringService.calculate(performance)` for each eligible player across each real match in that matchday.

---

## 12. Required Future Implementation Tasks

This specification will be implemented in the slices below. Each slice is its own pull request with its own tests.

### Implementation Plan

1. **Matchday entity and lock timestamps**
   - `MatchdayEntity { id, number, phase (GROUP/R16/QF/SF/FINAL), lockAt, status }`
   - `RealMatchEntity { id, matchdayId, kickoff, homeTeam, awayTeam, status (SCHEDULED/IN_PROGRESS/FINISHED/CANCELLED/POSTPONED) }`
   - Job to set `matchday.lockAt = min(realMatch.kickoff)` per matchday.

2. **Player match statistics model**
   - `PlayerMatchStatsEntity` storing every field consumed by `PerformanceStats`.
   - Mapper from entity to `PerformanceStats` record.
   - `onPitchGoalsConceded` derivable from minute-in / minute-out vs. goal timeline.

3. **Scoring rules configuration**
   - Flyway migration creating the `scoring_rules` table (with `position`, `threshold`, `bucket_size`, `event_scope`, `enabled`) and inserting default rows.
   - `ScoringRuleEntity`, repository, `ScoringRulesProvider` with typed accessors.
   - Admin-only endpoint (or DB-only, configurable) to change values.

4. **Score calculation service**
   - Rewrite `FantasyScoringService` to take `PerformanceStats` and return `ScoreBreakdown`.
   - Remove constants from `ScoringRules` once the provider is wired.

5. **Lineup aggregation service**
   - `MatchdayAggregationService` with lock, incomplete-lineup, and negative-balance rules.
   - Persists `MatchdayScoreEntity { userId, leagueId, matchdayId, total, reason }`.

6. **Market and clause rules**
   - Move `releaseClause` multiplier and resale ratio from hardcoded constants into `LeagueRules`.
   - (Already noted in `arevisar.md` 2026-05-17.)

7. **Frontend display**
   - Per-player breakdown component using `ScoreBreakdown.byCategory`.
   - Matchday total banner; reason chip when total is forced to 0 (incomplete / negative / eliminated / DNP).
   - Money formatting: thousands separator `.` and `M` suffix (e.g. `1.000.000` → `1M`).

8. **Tests** — see §13.

---

## 13. Required Tests

### 13.1 Unit tests — `FantasyScoringServiceTest`

One test per scenario. Each test asserts both `total` and the relevant `byCategory` entries.

- Goalkeeper save scoring: 2, 4, 6, 8 saves → 1, 3, 4, 6.
- Goalkeeper penalty save during match → +5.
- Goalkeeper penalty save in shootout → +3 (and NOT counted as match penalty save).
- Penalty shootout goal → +1 (not a normal goal).
- Penalty shootout miss → −1.
- Player scoring in extra time → normal-time values apply.
- Player did-not-play → 0.
- Player eliminated from tournament → 0 (asserted at the aggregator level via `MatchdayEligibility.eliminated`).
- Matchday 7 — only players whose team is in the final OR third-place match score; all others → 0 (asserted at the aggregator level).
- Clean sheet with ≥ 60 minutes for each of GK/DEF/MID/FWD.
- Clean sheet lost when player is on pitch and team concedes.
- Clean sheet retained when player completed 60 minutes and was substituted off before the goal.
- Goal by each position uses the position multiplier.
- Penalty goal in normal time uses +3, not the position multiplier.
- Goals-conceded pairs: 1, 2, 3, 4 conceded → 0, −2, −2, −4 for GK/DEF.
- Cards: yellow / double-yellow / direct red.
- Optional rules disabled contribute 0 even when the stat is non-zero.

### 13.2 Integration tests — `MatchdayAggregationServiceTest` (slice 5)

- Incomplete lineup at `lockAt` → 0 matchday points.
- Negative balance at `lockAt` → 0 matchday points (reason recorded).
- Locked lineup of 11 with mixed scores → sum matches per-player computation.
- Edits after `lockAt` do not affect current matchday.
- Cancelled real match → players from that match contribute 0.

### 13.3 Configuration tests — `ScoringRulesProviderTest`

- Defaults seeded when table is empty.
- Disabled rules are not applied.
- `reload()` picks up updated values without restart.

---

## 14. Out of Scope (this specification)

- Any code change. This document is specification only.
- Marca Fantasy parity — explicitly rejected as a goal.
- **Captain multipliers and any other point-multiplier mechanics.** Not implemented, not configured, not exposed in DTOs. May be added in a later, separate spec.
- Per-league override of scoring values (global rules first; league overrides may be added later).
- Frontend rules page (covered in slice 7).
- Importing `fant/players.json` and `fant/public/players/*` images (separate slice).
- Money formatting helper (`1.000.000` → `1M`) — implemented in slice 7, frontend only.

---

## 15. Open Items (to track in `arevisar.md`)

- Confirm exact source of `realMatch.kickoff` per the chosen data source.
- Decide whether `lockAt` snaps to the first kickoff of the matchday window OR each player locks at his own team's kickoff. This spec assumes the former.
- Decide whether negative-balance zeroing applies retroactively if balance turns negative after `lockAt`. This spec says no — only the value at `lockAt` matters.
