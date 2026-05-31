# Agent Response Log

This file records decisions, assumptions and deviations made during development.

---

## [2026-05-17] Initial build

**Task:** Full MVP build from scratch (T01–T21).

**Tech stack decisions:**
- Frontend: Next.js 14 App Router + TypeScript + Tailwind + shadcn/ui
- Backend: Java 21 + Spring Boot 3.x
- Database: PostgreSQL 15 + Flyway
- Auth: Spring Security + JWT + Google OAuth2
- 3D: three.js + @react-three/fiber + @react-three/drei
- Animations: motion (framer-motion)
- Forms: react-hook-form + zod
- Server state: @tanstack/react-query
- Client state: zustand (UI state only)

**Data source:** Starts as DATA_SOURCE_MODE=mock. MockSportsDataSourceImpl seeds 30+ players. Scraping deferred.

**Asset resolution:** All player images and team logos go through frontend/src/shared/assets/ resolvers. Components never access imageUrl directly. Safe mode flags in env.

**3D trophy:** Built from three.js primitives (LatheGeometry for cup body, TorusGeometry for handles/base). No official FIFA/World Cup geometry. Can be replaced with a .glb model later.

**Auth flow:** Email/password uses BCrypt + JWT. Google OAuth2 requires env credentials. Forgot-password requires SMTP config (see arevisar.md).

**Lineup validation:** MVP validates 11 players + user ownership. Formation rules deferred.

---

## [2026-05-17] Fix: JWT filter throwing 500 on stale tokens

**Symptom:** Every authenticated endpoint (leagues, market, lineup, ranking, profile, activity) returned HTTP 500. Backend log showed `UsernameNotFoundException: User not found: <uuid>` on every request.

**Root cause:** The JWT subject is a user UUID. `AppUserDetailsService.loadUserByUsername` calls `userRepository.findById(...).orElseThrow(UsernameNotFoundException::new)`. This exception was thrown from inside `JwtAuthenticationFilter` — a servlet filter that runs before the Spring DispatcherServlet, so `@RestControllerAdvice` (`GlobalExceptionHandler`) cannot intercept it. The exception bubbled to Tomcat → generic 500.

The token's UUID no longer existed in the DB (typical scenario: `docker-compose down -v` wiped Postgres while the frontend kept the old JWT in localStorage).

**Fix:** Wrapped the JWT resolution block in `JwtAuthenticationFilter` with try/catch. On any failure (invalid token, missing user, parse error) the SecurityContext is cleared and the request continues unauthenticated, so Spring Security returns a clean 401 for protected endpoints instead of 500. The frontend can then redirect to login.

**File:** `backend/src/main/java/com/fantasynations/security/JwtAuthenticationFilter.java`

---

## [2026-05-17] Slice A — Backend gaps for market/team UI and clause management

Implemented two missing backend pieces required by the Market UI countdown/money display and the "increase release clause" feature requested in `03_Game_RULES.md` ("Users can increase a player's release clause").

**New endpoints:**

- `GET /api/leagues/{leagueId}/me` → `LeagueMemberMeResponseDto { userId, leagueId, money, role, joinedAt }`. Lets the Market and Team tabs display the user's current available money without leaking other members' data. Returns 403 if the caller is not a member.

- `PATCH /api/leagues/{leagueId}/squad/{playerId}/clause` body `{ "releaseClause": <BigDecimal> }` → updated `SquadPlayerResponseDto`. Rules enforced:
  - caller must own the player in this league (404 otherwise);
  - new clause must be strictly greater than the current clause (400 otherwise — matches the doc, which only authorizes *increases*);
  - new clause must be positive (DTO `@DecimalMin 0.01`).
  - emits a new `RELEASE_CLAUSE_CHANGED` activity event with `playerName`, `previousClause`, `newClause`.

**Files touched:**
- `domain/ActivityEventType.java` (+RELEASE_CLAUSE_CHANGED)
- `dto/LeagueMemberMeResponseDto.java` (new)
- `dto/UpdateReleaseClauseRequestDto.java` (new)
- `service/LeagueService.java` + `LeagueServiceImpl.java` (+getCurrentMember)
- `service/SquadService.java` + `SquadServiceImpl.java` (+updateReleaseClause)
- `controller/LeagueController.java` (+GET /{id}/me)
- `controller/SquadController.java` (+PATCH /{playerId}/clause)

**Not in this slice (deferred to Slice B / C / D):**
- Frontend wiring (Market countdown, money display, clause-edit modal, league-settings page, light theme, header with user/page name).
- Removing the manual `POST /api/leagues/{id}/market/refresh` exposure from the UI (still keeps the scheduled @midnight job).

**Build:** `mvn clean compile` → BUILD SUCCESS (90 sources).

---

## [2026-05-17] Slice B — Market UI: real money, live 24h countdown, no manual refresh

Wired the Market screen to the Slice A backend so it shows authoritative data.

**Changes:**

- Summary bar at the top of `/leagues/[leagueId]/market` now shows:
  - "Your money" — fetched from `GET /api/leagues/{id}/me` (real `LeagueMember.money`, full euro formatting).
  - "Next refresh in: XXh XXm" — computed live from `MIN(availableUntil)` across the current market entries, ticking every 30 s. When the countdown elapses, the page re-fetches the market 5 s later so the new entries from the scheduled daily refresh appear automatically.

- Player price comes from `MarketPlayerResponseDto.price`, which the backend sets to `player.currentValue` — no client-side estimation or hardcoded values. Compact format is shown to fit mobile cards; full euro value is in the cell's `title` tooltip.

- "Buy" button is now disabled when `me.money < price` (prevents pointless 400s from the server's "Insufficient funds" check).

- Removed the manual "Refresh" button from the UI per spec. The `POST /market/refresh` endpoint is still on the backend (untouched in this slice) — only the UI no longer exposes it. The `@Scheduled(cron = "0 0 0 * * *")` job in `MarketServiceImpl` remains the source of truth for refresh cadence.

- Removed `refreshMarket()` from `frontend/src/shared/api/marketApi.ts` (no remaining callers).

- After a buy, invalidates `market`, `squad`, and `membership` queries so the money chip updates immediately.

**Files touched:**
- `frontend/src/shared/api/leagueApi.ts` (+`getMyMembership` + `LeagueMembership` type)
- `frontend/src/shared/api/marketApi.ts` (-`refreshMarket`)
- `frontend/src/app/leagues/[leagueId]/market/page.tsx` (rewrite)

**Type-check:** `tsc --noEmit` → clean.

**Not in this slice:**
- Money chip on the Team tab (Slice E).
- Owner-only gate on the `POST /market/refresh` endpoint, if we decide to keep it (open question — UI no longer calls it).

---

## [2026-05-17] Slice C — Lineup modal flow with backend position validation

Replaced the "tap a chip then tap a slot" interaction with the spec's flow: tap a slot → modal listing the user's eligible players for that slot's position → tap a player → assigned. Hardens the backend so that invalid slot/position combinations are rejected.

**Backend:**

- `LineupValidator` now:
  - parses the slot prefix (`GK-1`, `DEF-2`, `MID-3`, `FWD-1`) into a `PlayerPosition`;
  - rejects unknown prefixes (`400 Unknown slot position`);
  - rejects duplicate slots (`400 Slot used more than once`);
  - looks up each player and rejects a player placed in a slot whose `PlayerPosition` doesn't match (`400 Player X (FWD) cannot be placed in a MID slot`);
  - keeps the existing checks: exactly 11 players, all owned by the user's squad.
- Required new dependency in the validator: `PlayerRepository`.

The save endpoint (`PUT /api/leagues/{id}/lineup`) was untouched — the validator runs inside `LineupServiceImpl.saveLineup`.

**Frontend (`frontend/src/app/leagues/[leagueId]/lineup/page.tsx`):**

- Loads the saved lineup via `GET /api/leagues/{id}/lineup` and pre-fills the pitch (this was missing before — saved lineups didn't render on reload).
- Each pitch slot is a single button. Tap → opens `PlayerPickerModal`. The modal:
  - filters the squad to only show players whose `position` matches the slot's position;
  - sorts alphabetically;
  - shows an "in DEF-2" badge when a player is already placed in another slot — picking moves them rather than duplicating;
  - empty state when the user owns no players for that position;
  - Esc-to-close + click-on-backdrop-to-close + sheet-style on mobile / centered on desktop.
- Slot already filled: shows the player avatar + a small `×` button to clear that slot.
- Save button is disabled until `filled === 11`. Server-side validation errors are surfaced to the user via the API error message (rather than a generic toast).

**Files touched:**
- `backend/src/main/java/com/fantasynations/validation/LineupValidator.java`
- `frontend/src/app/leagues/[leagueId]/lineup/page.tsx`

**Build/checks:** `mvn clean compile` → success. `tsc --noEmit` → clean.

**Not in this slice:**
- Formation rules (`LeagueRules.formationRulesEnabled`) — the current pitch is a fixed 4-3-3. Formation validation is explicitly deferred per `arevisar.md`.
- Drag-and-drop (doc says it's optional, deferred).

---

## [2026-05-17] Slice D — Light theme + polished header + landing animation polish

Visual rework toward the "Linear / Vercel / Stripe" feel requested in the prompt, without inventing new functionality.

**Dependencies installed** (justified by this slice only):
- `gsap@3.15.0` — used for one scroll-scrubbed parallax in the hero.
- `lenis@1.3.23` — smooth scroll for the landing page only.
Both gated behind `prefers-reduced-motion: reduce` so they don't fire for users who opted out.

**Theme:**
- `globals.css` rewritten with a light token set:
  - `--background #fafafa`, `--surface #ffffff`, `--border #e5e7eb`, `--foreground #0f172a`, `--foreground-muted #475569`, soft Linear-style shadows.
  - Inter font stack with cv02/03/04/11 OpenType features for refined numerals.
  - Lenis css helpers (`html.lenis`, `lenis-smooth`, `lenis-stopped`).
  - Reduced-motion guard on `html { scroll-behavior }`.
- App pages swept from dark slate to light zinc surfaces:
  - `bg-slate-950/900/800` → `bg-zinc-50` (canvas) / `bg-white` (cards) with `border border-zinc-200` and `shadow-sm`.
  - `text-white` → `text-zinc-900`; `text-slate-*` → `text-zinc-500/600/700`.
  - Accent colors made readable on white: `text-blue-400` → `text-blue-600`, `text-amber-400` → `text-amber-500`, `text-emerald-400` → `text-emerald-600`, error → `text-rose-600`.
  - Position badges (GK/DEF/MID/FWD) switched from solid dark fills to soft pill colors (`bg-amber-100 text-amber-800`, `bg-sky-100 text-sky-800`, `bg-emerald-100 text-emerald-800`, `bg-rose-100 text-rose-800`) — same data, cleaner read.
  - Player avatars keep `text-white` on the deterministic colored circle from `getAvatarColor` (no contrast regression).
  - Lineup pitch stays dark green by design (it's a football pitch, not chrome) — only the header, modal, and outside-pitch text moved to light.

**Polished league header (`LeagueHeader.tsx`):**
- Sticky `bg-white/90 backdrop-blur border-b border-zinc-200`.
- Two-line title: league name (small, muted) + current page name derived from the route (`Team / Lineup / Ranking / Market / Activity`).
- User avatar + nickname on the right (avatar uses `getAvatarColor` + initials, falls back to `avatarUrl` when set). Nickname hidden on `< sm` viewports to keep mobile clean.
- Back chevron to `/leagues`.

The `/leagues` page header gained the same treatment: sticky white bar, user nickname + avatar, logout button.

**Landing polish:**
- `SmoothScrollProvider` (`landing/SmoothScrollProvider.tsx`): Lenis instance ticking via `requestAnimationFrame`, scoped to landing only via the page.tsx wrapper. Skips entirely when reduced motion is set. Cleans up on unmount.
- Hero (`HeroSection.tsx`): kept existing motion entrance animations (these handle the initial reveal best) and added GSAP `ScrollTrigger` with `scrub: true` for a subtle parallax — trophy drifts up faster than text as the section scrolls out. All animations registered through `gsap.context(sectionRef)` so cleanup is automatic on route change.
- Hero gradient flipped to white → zinc-100, stadium grid lines kept but at 6% opacity (was 10% over dark) so they read as architecture, not noise.
- Description cards: white surface, subtle 1px zinc border, on hover get `-translate-y-0.5 + shadow-md + border-blue-300` — Vercel-style "lift on hover" microinteraction.
- About section: white surface; GitHub button now uses `bg-white + border-zinc-200 + shadow-sm` instead of dark; LinkedIn keeps the blue accent.

**Files touched:**
- `src/app/globals.css`
- `src/app/page.tsx`
- `src/app/landing/{LandingHeader,HeroSection,DescriptionSection,AboutSection,SmoothScrollProvider}.tsx`
- `src/app/leagues/page.tsx`
- `src/app/leagues/[leagueId]/{layout,components/LeagueHeader,components/LeagueBottomNav}.tsx`
- `src/app/leagues/[leagueId]/{market,team,lineup,ranking,activity}/page.tsx`
- `src/app/leagues/components/{LeagueCard,CreateLeagueModal,JoinLeagueModal}.tsx`
- `src/app/profile/page.tsx`
- `src/shared/auth/{AuthModal,LoginForm,RegisterForm,ForgotPasswordForm}.tsx`

**Type-check:** `tsc --noEmit` → clean.

**Not in this slice (deferred to Slice E or future):**
- Team-tab clause-edit UI (calls the Slice A PATCH endpoint) — Slice E.
- League Settings page (calls the existing PUT `/settings`) — Slice E.
- Money chip on the Team tab — Slice E.
- 3D trophy material — kept gold/metallic; reads fine on the light background thanks to the `Environment preset="city"` in `TrophyCanvas`.
- Dark mode toggle — the spec mentions both light and dark "easy future theme changes", but only the light theme is wired now. The CSS variables make adding `:root[data-theme=dark]` later a one-file change.
- shadcn `button`/`dialog`/`sheet` primitive variants — left as default; they pick up the new tokens via Tailwind utilities passed at usage sites.

---

## [2026-05-17] Slice E — Team-tab clause edit + League Settings page

Closes the final gap from the original prompt: surfaces the Slice A backend endpoints in the UI.

**Team tab (`team/page.tsx`):**
- Adds a "Your money" chip backed by `GET /api/leagues/{id}/me` (same source the Market uses, so values are consistent across tabs).
- Each player row's "Clause" value is now a button with a pencil icon. Clicking opens `UpdateClauseModal`, which:
  - pre-fills with `currentClause + 1` (smallest valid increase);
  - blocks submission if the entered amount is not strictly higher than the current clause (matches the server's `BadRequestException`);
  - calls `PATCH /api/leagues/{id}/squad/{playerId}/clause` via the new `updateReleaseClause()` API helper;
  - on success invalidates `squad` and `activity` queries so both views refresh;
  - surfaces server-side error messages verbatim instead of a generic toast.
- Modal is keyboard-friendly (Esc, autofocus on input) and sheet-style on mobile.

**League settings page (`settings/page.tsx`):**
- New route `/leagues/[leagueId]/settings`.
- Fetches `league` + `me`. Renders the form only when `me.role === "OWNER"`; otherwise shows a clear "Only the league owner can change these settings — ask {ownerNickname}" message. The backend already enforces the same rule (`updateLeagueSettings` throws `ForbiddenException`), so the UI gate is purely cosmetic, not a security boundary.
- Form fields cover every property of `LeagueRules`:
  - starting money, money per point;
  - release-clause protection (hours);
  - market refresh interval (hours);
  - market players count;
  - max players per squad;
  - lineup size;
  - "enforce formation rules" checkbox.
- Validation via `react-hook-form` + `zod` (positive / non-negative / integer where appropriate). Number inputs use `valueAsNumber: true` so the form state stays numeric and matches the backend `LeagueRules` shape directly.
- Submit calls `updateLeagueSettings(leagueId, values)` (`PUT /api/leagues/{id}/settings`) and seeds the React-Query cache with the server's updated `League`; also invalidates `activity` because the backend emits a `RULES_CHANGED` event.
- "Discard" resets the form to the currently saved rules. Save/Discard disabled while pristine.

**LeagueHeader:**
- Loads `me` and shows a `Settings` cog link on the right (between the page title and the user avatar) only when `me.role === "OWNER"`. Hidden when already on the settings page so the chevron does the back nav.
- `Settings` added to `PAGE_LABELS` so the header label reads correctly on `/leagues/{id}/settings`.

**API helpers added (`shared/api/`):**
- `squadApi.updateReleaseClause(leagueId, playerId, amount)`.
- `leagueApi.updateLeagueSettings(leagueId, rules)`.

**Files touched:**
- `frontend/src/shared/api/squadApi.ts`
- `frontend/src/shared/api/leagueApi.ts`
- `frontend/src/app/leagues/[leagueId]/team/page.tsx`
- `frontend/src/app/leagues/[leagueId]/settings/page.tsx` (new)
- `frontend/src/app/leagues/[leagueId]/components/LeagueHeader.tsx`

**Type-check:** `tsc --noEmit` → clean.

**Status of original prompt:** all 11 items addressed across Slices A–E. Open follow-ups (already in `arevisar.md`): hardcoded `*1.5` / `*0.8` coefficients in market/clause flows, empty `iadocs/05_Model.md` / `06_Agent_Workflow.md`, possibility of removing `POST /market/refresh` entirely now the UI doesn't call it.

**Market:** Refreshes every 24 hours via Spring @Scheduled. Initial market populated from mock data source.

**Scoring:** Deterministic algorithm in scoring/FantasyScoringService. Not connected to live data yet.

### [2026-05-18] World Cup scoring & matchday — specification only

**Decision:** Wrote `iadocs/specs/2026-05-18-worldcup-scoring-design.md` covering matchdays, lock rules, base/GK/clean-sheet/conceded/shootout/optional scoring, configurable rules via a future `scoring_rules` table, and an 8-slice Implementation Plan. No code in this slice.

**Assumptions logged in spec §15 (open items):** matchday `lockAt` snaps to the first kickoff of the matchday window; negative-balance check uses the value at `lockAt` only.

**Deferred:** all implementation (entities, DTOs, service rewrite, tests), `fant/players.json` import, and the `1.000.000` / `M` money formatter.

### [2026-05-18] World Cup scoring spec — revisions applied

**Changes:** Strengthened the no-hardcoded-values rule (covers point values, thresholds, bucket sizes). Extended `scoring_rules` columns with `position`, `threshold`, `bucket_size`, `event_scope`. Switched schema creation to a future Flyway migration (no JPA auto-DDL, no code seeder). Defined complete-lineup as 11 players + valid formation; aggregator must reject otherwise. Added matchday-7 rule (only final / third-place participants score). Moved `eliminated` (and added `qualifiedForRound`, `matchday7Slot`) into a new `MatchdayEligibility` DTO consumed by the aggregator. Captain multipliers explicitly out of scope.

### [2026-05-18] Slice 1 — Matchday + scoring rules infrastructure

**Implemented:**
- Flyway `V12__create_matchdays.sql` (matchdays + real_matches tables, seeds 7 matchdays).
- Flyway `V13__create_scoring_rules.sql` (scoring_rules with code/value/category/position/threshold/bucket_size/event_scope/enabled, seeds all spec defaults; optional bonuses seeded disabled).
- Enums: `MatchdayPhase`, `MatchdayStatus`, `RealMatchStatus`, `ScoringRuleCategory`, `EventScope`, `ScoringRulePosition`.
- Entities: `MatchdayEntity`, `RealMatchEntity`, `ScoringRuleEntity`.
- Repositories: `MatchdayRepository`, `RealMatchRepository`, `ScoringRuleRepository`.
- `MatchdayLockService.recalculate(matchdayId)` sets `lockAt = min(realMatch.kickoff)`, ignoring CANCELLED and POSTPONED matches.
- `ScoringRulesProvider` loads all rules on `@PostConstruct`, exposes typed accessors (`pointsPerGoal`, `cleanSheet`, `concededBucketSize`, `saveBucketValue`, `shootoutGoal`, ...), `reload()` for live updates, raises `IllegalStateException` for missing threshold/bucket lookups, returns 0 for disabled/unknown rules.

**Tests:** `ScoringRulesProviderTest` (10), `MatchdayLockServiceTest` (7). All 17 green via `mvn test`.

**Deferred to next slices:** `FantasyScoringService` rewrite to consume `PerformanceStats`, `MatchdayAggregationService`, `PlayerMatchStatsEntity`. Old hardcoded `ScoringRules` constants are still present and untouched until the engine consumes the provider.

### [2026-05-18] Ticket 1 — World Cup player import

**Implemented:**
- Flyway `V14__players_unique_name_team.sql` adds `UNIQUE (name, national_team)` for idempotent upserts.
- `PlayerRepository.findByNameAndNationalTeam`.
- `WorldCupPlayerDto`, `WorldCupPlayersFile`, `PlayerPositionMapper` (GK/DF/MF/FW + Portero/Defensa/Defensor/DefensorDefensor/Centrocampista/Medio/Delantero), `PlayerJsonImporter` (idempotent upsert by `(name, national_team)`, accepts null `image_url`, default `base_value`/`current_value` = 1_000_000).
- `PlayerImportRunner` (`ApplicationRunner`) gated by `app.players.import-on-startup` (default true). JSON path from `app.players.json-path` (default `../fant/players.json`, env `PLAYERS_JSON_PATH`).
- Missing/unreadable JSON logs a clear error and lets the app keep starting (no crash).
- Legacy `DataSourceInitializer` mock seeder now disabled by default; gated by `app.players.mock-seed-on-startup`.
- Frontend `scripts/sync-players.mjs` mirrors `../fant/public/players` to `frontend/public/players`. Wired into `predev`/`prebuild`. `frontend/public/players/` gitignored.
- `playerImageResolver` passes `/players/...` refs through to Next.js public.
- `startup.md` documents JSON path + Docker mount instructions.

**Tests:** `mvn test` → 42/42 green (added `PlayerJsonImporterTest` x7, `PlayerPositionMapperTest` x17; pre-existing 18 still pass).

**Notes:**
- `DATA_SOURCE_MODE` env var kept for compatibility but no longer drives player seeding.
- Importer never deletes existing rows; players removed from JSON simply stop being touched. Cleanup of stale fictional players is a separate ticket if needed.
- Old hardcoded `ScoringRules` constants still present — will be removed when the scoring engine is rewritten in a later ticket.

### [2026-05-18] Ticket 2 — Market auto-initialised on league creation

**Implemented:**
- New `MarketResponseDto { available, nextRefreshAt, players, reason }` replaces the bare list response. `reason` is `null`, `NO_PLAYERS_IN_POOL`, or `NOT_ENOUGH_PLAYERS`.
- `MarketService.initializeMarketIfMissing(leagueId)` - refreshes only when no active entries exist, so reruns never duplicate cycles.
- `MarketRepository.countByLeagueIdAndAvailableUntilAfter` for the gate.
- `MarketServiceImpl.getMarket` computes `nextRefreshAt = min(availableUntil)`, defensively calls `initializeMarketIfMissing` to backfill leagues created before this change, and returns the structured empty-state when the pool is empty or smaller than the configured size.
- `LeagueServiceImpl.createLeague` calls `marketService.initializeMarketIfMissing(league.getId())` after the squad is saved.
- `MarketController` returns `MarketResponseDto`.
- Frontend `marketApi.ts` updated to the new shape. `market/page.tsx` reads `data.players` and `data.nextRefreshAt`, plus a distinct empty-state message when `reason == NO_PLAYERS_IN_POOL`.

**Tests:** `mvn test` → 49/49 green (added `MarketServiceImplTest` x7 covering: empty-pool empty-state, partial pool flagged, idempotent re-init, available/nextRefreshAt populated, forbidden non-member).

**Notes:**
- Existing `refreshMarket` still wipes and recreates; manual refresh button keeps working.
- Scheduled daily refresh untouched.
- No DB migration required.

### [2026-05-18] Ticket 3 — Internationalization (en / es / gl)

**Implemented:**
- Custom lightweight i18n with no extra runtime dep. New module `frontend/src/shared/i18n/`:
  - `locales/en.ts` (source of truth + `Dictionary` type via `WidenStrings<typeof en>`), `locales/es.ts`, `locales/gl.ts`.
  - `I18nProvider.tsx` — `lang`, `setLang`, `t(key, vars)`; default `en`; localStorage key `fn:lang`; updates `document.documentElement.lang` on change. SSR-safe (renders default on first paint, hydrates to stored language).
  - `LanguageSelector.tsx` — compact and full variants. Uses native `<select>` so it works without extra components.
- Wired `<I18nProvider>` inside `RootLayout` (under `<QueryProvider>`).
- Selector placed in landing header (desktop + mobile), leagues list header, and profile page.
- Localized strings on: landing header, `DescriptionSection`, `AboutSection`, `AuthModal`, `LoginForm`, `RegisterForm`, `ForgotPasswordForm`, leagues list page, `CreateLeagueModal`, `JoinLeagueModal`, `LeagueCard`, `LeagueHeader` (tab title + ARIA), `LeagueBottomNav`, team page, lineup page, ranking page, activity page (event labels via dictionary with variable interpolation), settings page (title + back), profile page.
- Player names, national team names, invite codes and currency values remain data, not translation keys (per ticket).

**Verification:**
- `pnpm exec tsc --noEmit` → 0 errors.
- `mvn test` (backend) → 49/49 green (no regression).

**Notes:**
- Selected language persists across reloads via `localStorage`. Switching is instant (no reload) because the provider re-renders the tree.
- A few low-traffic strings (e.g. the four feature-card bodies on the landing page) are still in English; the screen is not fully hardcoded since title/lead/header/nav/auth are localized. Easy to expand the dictionary in a follow-up.
- Toasts and Zod field errors are localized indirectly: schemas store keys (e.g. `passwordMin`), forms resolve them via `t("auth.<key>")`.

### [2026-05-18] docker-compose: mount fant/ into backend

Added `./fant:/app/fant:ro` mount and `PLAYERS_JSON_PATH=/app/fant/players.json` (+ `PLAYERS_IMPORT_ON_STARTUP=true`) to the backend service so ticket 1 import works in Docker without any extra setup.

### [2026-05-18] docker-compose: mount fant/public/players into frontend

Added `./fant/public/players:/app/public/players:ro` to the frontend service. Previously the frontend Docker image was built from `./frontend` only, so player images were never copied in and every `/players/<team>/<player>.png` returned 404. The mount serves them at runtime from the canonical fant/ folder. No image rebuild needed.

### [2026-05-18] Market value system — slice A

**Implemented:**
- Flyway `V15` adds `initial_market_value`, `market_value`, `importance`, `league_reputation`, `availability_status` to `players` and widens money columns to `DECIMAL(18,0)`.
- Flyway `V16` creates `market_value_history` with `oldValue/newValue/delta/deltaPercent/momentumScore/reason/matchdayId/marketCycleId/breakdownJson`.
- Flyway `V17` widens remaining money columns and adds `fixed_release_clause_value` + `release_clause_manually_raised` to `squad_players`.
- Enums: `Importance`, `LeagueReputation`, `NationalTeamTier`, `AvailabilityStatus`, `MarketValueChangeReason`.
- `MarketValueConfig` — typed, holds every constant from the spec (position base, tier bonuses, importance/reputation bonuses, momentum thresholds, percent deltas, special-rule floors, caps, rounding unit, quick-sell 
### [2026-05-18] Market value system — slice A

**Implemented:**
- Flyway V15 adds initial_market_value, market_value, importance, league_reputation, availability_status to players and widens money columns to DECIMAL(18,0).
- Flyway V16 creates market_value_history (oldValue/newValue/delta/deltaPercent/momentumScore/reason/matchdayId/marketCycleId/breakdownJson).
- Flyway V17 widens remaining money columns; adds fixed_release_clause_value + release_clause_manually_raised to squad_players.
- Enums: Importance, LeagueReputation, NationalTeamTier, AvailabilityStatus, MarketValueChangeReason.
- MarketValueConfig — typed, holds every constant from the spec (position base, tier bonuses, importance/reputation bonuses, momentum thresholds, percent deltas, special-rule floors, caps, rounding unit, quick-sell %, machine-offer range, auto-clause multipliers, starting budget 200M). No magic numbers anywhere else.
- NationalTeamTierResolver maps team name to tier (S/A/B/C lists from spec).
- MarketValueCalculator — pure, deterministic. calculateInitial(...) and calculateDelta(...). Applies special-rule floors (injured -8%, suspended -5%, eliminated -10%, DNP -5%, rested-superstar 0%, strong-performance protection), hard caps ±15%, future bounds 1M–200M, rounds to 100k. Returns MarketValueResult { oldValue, newValue, delta, deltaPercent, momentumScore, reason, breakdown }.
- MarketValueSeedLoader reads optional fant/market-value-seed.json (env MARKET_VALUE_SEED_PATH). Missing/malformed file logs and returns empty map.
- InitialMarketValueService — wires calculator + seed + deterministic fallback importance (per-team-per-position counters); writes players row + one market_value_history row with reason INITIAL_VALUE.
- ReleaseClauseService — autoClause = marketValue * multiplier(importance); effective = max(market, auto, fixed?); applyManualRaise validates (cannot lower, cannot equal current), stores fixedReleaseClauseValue, marks releaseClauseManuallyRaised.
- PlayerJsonImporter now routes new players through InitialMarketValueService.applyForNewPlayer. Importance fallback runs deterministically per import via beginImportRun().
- LeagueRules.startingMoney default -> 200_000_000. Existing leagues keep stored value.

**Tests:** mvn test -> 90/90 green (49 prior + 41 new).
- MarketValueCalculatorTest (24): all initial-value scenarios + dynamic deltas, caps, special-rule floors, rounding, reason inference.
- InitialMarketValueServiceTest (3): algorithm applied + history persisted; deterministic fallback importance; seed-file override.
- ReleaseClauseServiceTest (10): auto follows market, fixed sticky, auto can exceed fixed, fall-back-to-fixed, manual-raise validation, recalc.
- MarketValueSeedLoaderTest (3): missing file, valid JSON, malformed JSON.
- Existing PlayerJsonImporterTest updated for new constructor; passes.

**Out of scope (slice B/C):** market cycles, secret bids, machine offers, quick sell, wiring DynamicMarketValueService into a scheduler, frontend changes. Existing buy-instantly market flow untouched. MarketServiceImpl.buyPlayer still uses its legacy price * 1.5 clause — will be replaced with ReleaseClauseService once squad/buy flow is reworked.

**Notes:**
- Money kept as BigDecimal with scale 0 (DECIMAL(18,0)). Not floating point; satisfies the spec.
- Legacy players.base_value/current_value are kept in sync with market_value/initial_market_value to keep older market code working.

### [2026-05-18] Market slice B — cycles, secret bids, machine offers, quick sell

**Implemented (backend only, no frontend changes):**

- Flyway V18 creates `market_cycles (id, league_id, cycle_number, opens_at, closes_at, resolved_at, status, created_at)` and adds `cycle_id` to `market_players`.
- Flyway V19 creates `bids (id, cycle_id, market_player_id, user_id, amount, status, submitted_at, resolved_at)` with UNIQUE (market_player_id, user_id).
- Flyway V20 creates `machine_offers (id, cycle_id, league_id, squad_player_id, seller_user_id, amount, status, expires_at, created_at, accepted_at)`.
- Enums: MarketCycleStatus (OPEN/RESOLVING/CLOSED), BidStatus (SUBMITTED/WON/LOST/REJECTED_NO_FUNDS), MachineOfferStatus (PENDING/ACCEPTED/EXPIRED).
- Entities + repositories for all three.
- MarketCycleService: getOrCreateOpenCycle creates cycle 1 lazily for any league, createNextCycle opens N+1 from a resolved cycle, findCyclesDueForResolution drives the scheduler. Cycle duration = LeagueRules.marketRefreshIntervalHours (default 24).
- BidService: secret bid submit/update. One bid per (listing, user); re-submission overwrites amount and resets submittedAt. Money is NOT debited at submission. Cross-league listing access is forbidden. Bidding rejected if cycle isn't OPEN.
- MachineOfferService: listForSale generates a deterministic offer at marketValue +/- 10% using hash(squadPlayer.id, cycle.id). Accept transfers ownership + credits seller. Prior pending offers on the same squad player are auto-expired.
- QuickSellService: 50% of marketValue, immediate. Credits user, deletes squad row, writes activity entry.
- MarketCycleResolutionService: resolve(cycleId) walks each listing, picks highest bid (earliest submittedAt breaks ties), tries the winner (next-highest takes over if balance is insufficient -> REJECTED_NO_FUNDS), executes transfer (debit, attach to squad, recalculate effective release clause via ReleaseClauseService, mark winner WON / losers LOST), expires pending machine offers, closes cycle, opens cycle N+1 and populates fresh listings via MarketListingPopulator. Re-running on a CLOSED cycle is a no-op. @Scheduled fixedDelay = 60s by default (overridable via app.market.resolution-interval-ms).
- MarketListingPopulator: extracted from old refreshMarket. Builds listings tied to a cycle, prices them at the player's global marketValue.
- MarketServiceImpl rewritten: getMarket now reads listings by cycle, attaches the caller's own bid amount to each MarketPlayerResponseDto, returns nextRefreshAt = cycle.closesAt, preserves NO_PLAYERS_IN_POOL / NOT_ENOUGH_PLAYERS reasons. initializeMarketIfMissing creates cycle 1 + populates listings. buyPlayer and refreshMarket removed from the service.
- MarketController rewritten: GET /market unchanged shape. POST /market/buy/{playerId} and POST /market/refresh REMOVED. New endpoints: POST /market/listings/{listingId}/bid, POST /squad/{squadPlayerId}/list-for-sale, POST /offers/{offerId}/accept, POST /squad/{squadPlayerId}/quick-sell.
- MarketPlayerResponseDto extended with ownBidAmount (nullable). MarketResponseDto unchanged.

**Tests:** mvn test -> 104/104 green (90 prior + 14 new).
- MarketCycleServiceTest (4): first-cycle creation, idempotent for existing OPEN cycle, next cycle after CLOSED, duration honours league rules.
- MarketCycleResolutionServiceTest (6): highest wins, tie -> earliest submittedAt, insufficient funds -> next-highest, no eligible bids -> listing dropped, re-resolve closed is no-op, next cycle opened + populated.
- MachineOfferServiceTest (3): 200 random ids always within +/-10%, deterministic per (squadPlayer, cycle), different cycles produce different offers.
- QuickSellServiceTest (2): 50% credit, forbidden for non-owner.
- MarketServiceImplTest rewritten (6) for the new cycle-aware flow.

**Behaviour notes:**
- Money is debited only at resolution. Bids can be submitted up to cycle.closesAt.
- Re-bidding overwrites the previous amount (one bid per user per listing); spec wording "highest bid wins" stays correct.
- Transfers are immediate at cycle close; "effective next cycle" is satisfied by the fact that cycle N+1 opens right after.
- Machine offer formula is intentionally deterministic per (squadPlayer, cycle) so tests can pin amounts; it still feels random to users.
- The legacy daily-cron market refresh was removed; cycle resolution now drives all listing changes.

**Out of scope (next slice / follow-up):**
- Frontend: market page still calls the old buy endpoint - needs a Bid form, list-for-sale button, quick-sell shortcut, accept-offer button.
- Admin UI for cycle duration / market size.
- Dynamic market value updates between cycles (slice C, depends on scoring).

### [2026-05-18] Market slice B — frontend follow-up

**Implemented:**
- marketApi.ts: removed buyPlayer; added placeBid, listSquadPlayerForSale, acceptMachineOffer, quickSell; extended MarketPlayer with ownBidAmount.
- Market page rewritten:
  - Replaced Buy button with a per-listing bid form (numeric input + Place bid / Update bid button).
  - Shows the user's own bid as "Your bid: N (pending)" next to the form when ownBidAmount is set.
  - Header banner "Bids are secret and resolved at the end of the market cycle. The highest bid wins." (Lock icon).
  - "Cycle ends in HHh MMm" countdown stays wired to nextRefreshAt; label changed from "Next refresh in" to "Cycle ends in".
  - Disables submit when the input is invalid or exceeds the user's money; toasts via i18n.
  - Empty / NO_PLAYERS_IN_POOL states preserved.
- Team page extended:
  - Each squad row now has two extra actions: "List for sale" and "Quick sell".
  - List for sale -> POST /squad/{id}/list-for-sale -> shows a modal with the machine offer amount and an Accept button (POST /offers/{offerId}/accept). Closing the modal leaves the offer pending; it auto-expires at cycle close.
  - Quick sell -> confirm at 50% of currentValue -> POST /squad/{id}/quick-sell -> toast + invalidate squad/membership/activity queries.
- i18n: added market keys (secretNotice, bidLabel, bidPlaceholder, bidSubmit, bidUpdate, bidPending, bidConfirm, bidUpdated, bidSubmitted, bidFailed, bidMustBePositive, bidStartingPrice, notEnoughMoney; renamed nextRefresh/empty wording) and team keys (listForSale, listForSaleConfirm, listingFailed, machineOffer, acceptOffer, acceptOfferConfirm, offerAccepted, offerFailed, offerExpires, quickSell, quickSellConfirm, quickSellHint, quickSellDone, quickSellFailed) in en/es/gl.
- No remaining references to POST /market/buy/{playerId} or POST /market/refresh anywhere in frontend/src.

**Verification:**
- pnpm exec tsc --noEmit: 0 errors.
- pnpm exec next build: success (all 6 dynamic + 4 static routes compile).

**Out of scope (next):**
- Slice C: dynamic market value updates between cycles (depends on scoring).
- Surfacing the bid history / WON / LOST result toasts when a cycle resolves (today the user just sees the player in their squad on next reload).

### [2026-05-18] Scoring & matchday slice

**Implemented (backend, no frontend changes in this slice):**

- Flyway V21: player_match_stats (per player per real-match, every spec field).
- Flyway V22: locked_lineup_players (frozen snapshot), matchday_scores (per user per matchday), player_matchday_scores (per player per matchday with breakdown_json), lineups.frozen_at + lineups.frozen_for_matchday_id.
- Domain enums: MatchdayAggregationReason (OK / INCOMPLETE_LINEUP / NEGATIVE_BALANCE), Matchday7Slot.
- DTOs: PerformanceStats, MatchEvents, ShootoutEvents, OptionalStats, MatchdayEligibility, ScoreBreakdown. Eligibility is separate from PerformanceStats; the scoring engine is tournament-agnostic.
- Entities + repos: PlayerMatchStatsEntity, LockedLineupPlayerEntity, MatchdayScoreEntity, PlayerMatchdayScoreEntity.
- LineupEntity extended with frozenAt and frozenForMatchdayId.
- FantasyScoringService rewritten as a pure calculator that consumes PerformanceStats and ScoringRulesProvider. Returns ScoreBreakdown (total + map by category). Order of evaluation matches the design doc. Removed legacy ScoringRules constants file.
- PlayerMatchStatsMapper (entity -> PerformanceStats).
- LineupFreezeService: snapshot once at first aggregation, idempotent reuse, FE-visible frozenAt timestamp on the lineup.
- MatchdayAggregationService: aggregates one user (or every league member) for a matchday. Lineup snapshot is the only source of truth; later edits, transfers, clauses, market purchases, quick-sells do NOT affect that matchday. Incomplete snapshot -> 0 with INCOMPLETE_LINEUP. Negative balance at aggregation -> 0 with NEGATIVE_BALANCE. Re-aggregation reuses the snapshot and overwrites totals (useful when stats are corrected). Persists per-player breakdown JSON.
- PlayerMatchStatsService: upsert by (player, real_match).
- MatchdayScoreReader: read-only assembler for the frontend "Matchday Scores / Puntuaciones" view. Returns the locked lineup with per-player points and breakdown.
- AdminScoringController:
  - POST /api/internal/matches/{realMatchId}/stats             upsert one player's stats
  - POST /api/internal/matches/{realMatchId}/stats/bulk        upsert many
  - POST /api/internal/leagues/{leagueId}/users/{userId}/matchdays/{matchdayId}/aggregate
  - POST /api/internal/leagues/{leagueId}/matchdays/{matchdayId}/aggregate
- MatchdayScoreController (read endpoint exposed to the FE):
  - GET /api/leagues/{leagueId}/matchdays/{matchdayId}/score                      caller's own frozen lineup
  - GET /api/leagues/{leagueId}/matchdays/{matchdayId}/score/by-user/{userId}     another member's
- MatchdayAggregationRunner: @Scheduled (fixedDelay default 5 min) auto-aggregates any matchday whose real_matches are ALL FINISHED, for every league.

**Tests:** mvn test -> 129/129 green (104 prior + 25 new).
- FantasyScoringServiceTest (16): DNP, minutes bands, position goals, penalty goal beats position multiplier, GK save buckets across 2/4/6/8 saves, GK pen save in match, shootout goal/miss, shootout save separate from match pen save, clean sheet per position, clean sheet ignored under 60', goals conceded pairs, cards, own goal, disabled optional rules contribute 0, extra-time treated as normal time.
- LineupFreezeServiceTest (4): first freeze snapshots live lineup, re-freeze reuses snapshot, empty live lineup -> empty snapshot, separate matchday -> separate snapshot.
- MatchdayAggregationServiceTest (5): incomplete lineup -> 0 INCOMPLETE_LINEUP, negative balance -> 0 NEGATIVE_BALANCE, happy-path sum of frozen lineup, re-aggregate reuses snapshot, post-lock lineup edits do not change matchday score.

**Behaviour confirmed:**
- User can keep editing the live lineup at any time.
- At first aggregation a frozen snapshot is created in locked_lineup_players.
- The snapshot is the only lineup used to calculate that matchday's score.
- A snapshotted player still scores even if he later leaves the squad via market/quick-sell/clause.
- Re-aggregating reuses the existing snapshot.
- Read endpoint exposes the frozen lineup + per-player breakdown for the FE.

**Out of scope (next):**
- Slice C: dynamic market value updates per matchday now that the scoring data exists.
- Frontend "Matchday Scores / Puntuaciones" section consuming GET /matchdays/{id}/score.
- Bracket / elimination data so MatchdayEligibility can be populated from real tournament state (the aggregator currently treats every player as eligible; the resolver hook is in place).

### [2026-05-18] Slice C — Dynamic market value updates after matchday aggregation

**Implemented:**
- DynamicMarketValueService: after a matchday is aggregated, walks every player who has stats in that matchday and applies MarketValueCalculator.calculateDelta with real stored data.
  - lastMatchdayPoints  = MAX of player_matchday_scores.points across leagues for this matchday (collapsed via JPQL GROUP BY).
  - averagePoints       = AVG of player_matchday_scores.points across every matchday with number STRICTLY LESS than this one (JPQL aggregate).
  - minutesPlayed       = SUM of player_match_stats.minutes_played across the FINISHED real matches of the matchday.
  - didNotPlay          = true when every stats row for the player is flagged did_not_play.
  - availability        = player.availabilityStatus (default AVAILABLE).
  - importance          = player.importance (nullable - calculator handles).
  - team elimination / semis / final = neutral fallback (false) until a bracket model exists.
  - demandScore = 0.
- Idempotency by (player, matchday) via new MarketValueHistoryRepository.existsByPlayerIdAndMatchdayId; a second run for the same matchday is a no-op for already-processed players.
- player.marketValue and player.currentValue are updated; player.initialMarketValue is NEVER touched (verified by test).
- MarketValueHistory rows always written (even on zero delta) with matchdayId tag and breakdown JSON.
- All SquadPlayerEntity rows referencing the updated player are recomputed via ReleaseClauseService.recalculate so effective release clauses follow the new market value.
- MatchdayAggregationRunner now calls DynamicMarketValueService.applyForMatchday after the aggregation loop for each due matchday.
- New admin endpoint: POST /api/internal/matchdays/{matchdayId}/apply-market-value (idempotent manual trigger).

**Repository additions:**
- MarketValueHistoryRepository.existsByPlayerIdAndMatchdayId
- MatchdayScoreRepository.findByMatchdayId
- PlayerMatchStatsRepository.findByRealMatchIdIn
- PlayerMatchdayScoreRepository.findPointsByMatchday (JPQL GROUP BY)
- PlayerMatchdayScoreRepository.findHistoricalAveragesBefore (JPQL AVG)

**Tests:** mvn test -> 137/137 green (129 prior + 8 new).
- DynamicMarketValueServiceTest:
  - high score increases marketValue and writes HIGH_PERFORMANCE history;
  - bad/negative score decreases marketValue with LOW_PERFORMANCE reason;
  - injured player floors at -8% with INJURED reason regardless of points;
  - suspended player floors at -5% with SUSPENDED reason;
  - history entry always created for processed players;
  - repeated apply for same matchday is idempotent (skipped via existsByPlayerIdAndMatchdayId);
  - release clauses recomputed via ReleaseClauseService after marketValue changes;
  - players with no stats are not in the processing universe.

**Behaviour confirmed:**
- initialMarketValue is never changed by the dynamic update.
- History rows are tagged with matchdayId, preventing duplicate application.
- Release clauses follow marketValue automatically (and the fixed-clause sticky logic from ReleaseClauseService still wins when set).
- Tournament-context resolution is wired but uses a neutral fallback; ready to be replaced when the bracket model lands.

**Out of scope (this slice):**
- Frontend display of market value changes / history (read endpoint can be added later with no engine change).
- Bracket / elimination data model for accurate team-eliminated / semi-finals / final flags.
- Restricting which calls can hit /api/internal/* in production - existing security config still applies.

### [2026-05-18] Initial economy — standard FantasyNations start (mandatory 15 random players + remaining cash)

**Behaviour change:** the user/admin no longer chooses starting money. Every user gets 15 random players plus the remaining cash up to {@code startingBudget} (default 200,000,000).

**Backend:**
- LeagueRules:
  - new `startingBudget` (200M) as the single, fixed total budget;
  - new `initialSquadSize` (15) and per-position counts (GK=2, DEF=4, MID=6, FWD=3);
  - new target range `initialSquadTargetMinValue` (100M) / `initialSquadTargetMaxValue` (150M);
  - legacy `startingMoney` kept for backwards compatibility with serialized rules JSON but marked `@Deprecated` and not read by any business code.
- `CreateLeagueRequestDto` no longer accepts `startingMoney`.
- `LeagueServiceImpl.createLeague` and `joinLeague` set the member's money to 0 placeholder and immediately call the new assignment service to populate the squad + final money.
- `LeagueServiceImpl.updateLeagueSettings` now PRESERVES the budget and initial-squad shape; it only honours editable league-shape fields (moneyPerPoint, market refresh interval, market size, etc.).
- `InitialSquadAssignmentService`:
  - Picks 15 players using the standard composition; falls back to ±1 on outfield positions (and on GK only when the GK pool is too thin).
  - Up to 60 random attempts to land in [100M, 150M]; injected `RandomGenerator` so tests are deterministic.
  - Tracks already-assigned players within the league (no duplicate ownership).
  - `constrainToBudget` swaps the most expensive picks for cheaper alternatives until the total fits under `startingBudget`.
  - Sets `member.money = startingBudget - squadMarketValue` (floor 0).
  - Recomputes the effective release clause for every assigned ownership via `ReleaseClauseService`.
- `RandomConfig` exposes a single `RandomGenerator` bean (`ThreadLocalRandom.current()`).

**Frontend:**
- `leagueApi.ts` types: `startingMoney` removed from `LeagueRules`; new `startingBudget` + `initialSquadSize/Gk/Def/Mid/Fwd`. `createLeague` payload no longer accepts `startingMoney`.
- `CreateLeagueModal`: starting-money input REMOVED. A short hint line explains the mandatory standard start (i18n key `leagues.standardStartHint` in en/es/gl).
- League settings page: starting-money field REMOVED. Replaced by a read-only info line showing the total budget per user; the budget is not editable. Form submit still preserves it via a server-side guard.

**Tests:** mvn test -> 145/145 green (137 prior + 8 new).
- InitialSquadAssignmentServiceTest:
  - assigns 15 players with the standard 2/4/6/3 composition;
  - starting money = budget - assignedSquadMarketValue;
  - no player is assigned twice in the same league (excludes already-owned players);
  - fallback composition keeps total at 15 when a pool is too small;
  - typical cheap pool produces a squad in the 100M-150M target range;
  - squad never exceeds the budget when alternatives exist (constrainToBudget swap);
  - weak pool gives extra money instead of failing;
  - every assigned ownership row receives a non-zero effective release clause.

**Frontend verification:** `pnpm exec tsc --noEmit` -> 0 errors.

**Out of scope (next):**
- Backfill / migration for existing leagues created with the old empty-squad flow (none exist in production yet; dev DB is wiped per project convention).
- An admin override mode is intentionally NOT implemented per the ticket.
- Frontend "Initial squad" highlight after league creation (separate small ticket if needed).

### [2026-05-18] Combined ticket: economy 250M tune + user listings on market (48h) + classification squad value

**Slice 1 — Economy retune**
- `LeagueRules`: `startingBudget` 200M → 250M; squad target band 150M-200M (was 100M-150M).
- `MarketValueConfig.futureMax` 200M → 250M to match the new budget ceiling.
- Tests updated: starting money expectation 100M (was 50M); typical pool test uses 12M players to land in the new band; weak-pool test expects 235M remaining.

**Slice 2 — User listings on the market**
- Flyway V23: `market_players.seller_user_id UUID NULL` (NULL = system / free market; non-null = user listing).
- `MarketPlayerEntity` gets `sellerUserId`.
- `MarketPlayerResponseDto` exposes `sellerUserId` + display-friendly `sellerNickname` (resolved server-side via `UserRepository`).
- New `UserMarketListingService` + endpoint `POST /api/leagues/{leagueId}/squad/{squadPlayerId}/list-on-market { askingPrice }`. Listings get a 48h `availableUntil`. The constant lives in `USER_LISTING_HOURS`.
- `MarketCycleResolutionService.executeTransfer` extended:
  - When `seller_user_id` is set: credit the seller with the winning bid, delete the seller's `SquadPlayer` for that player.
  - Free-market listings continue to work unchanged.
- Cycle resolver carries live user listings over to the next cycle (re-anchors `cycle_id`) so the 48h promise survives the 24h cycle close.
- Bids on user listings reuse the existing secret-bid + tie-breaker + insufficient-funds logic - no separate code path.

**Slice 3 — Squad value in standings**
- `RankingEntryDto` adds `squadValue` (BigDecimal). Distinct from `totalPoints`.
- `LeaderboardService` computes squad value as the SUM of `marketValue` for all players owned by each user in the league. Uses current `marketValue`, not `initialMarketValue`, per the spec.
- `SquadRepository.findByLeagueId` added for the aggregate.
- Works in both the snapshot path AND the day-one fallback path.

**Frontend**
- `marketApi.ts` exposes `sellerUserId`, `sellerNickname`, plus new `listOnMarket(leagueId, squadPlayerId, askingPrice)` helper.
- Market page: each listing card now shows a small badge - either "Free market" (with a globe icon) or "Listed by {nickname}" (with a user icon).
- `rankingApi.ts` adds `squadValue` to `RankingEntry`.
- Ranking page: under each user's nickname, a small emerald-coloured line shows "Squad value: 185M" (i18n-labelled). Points stay on the right in blue. Visually distinct.
- i18n keys (en/es/gl):
  - `league.ranking.squadValue` → "Squad value" / "Valor plantilla" / "Valor do cadro".
  - `league.market.freeMarket` → "Free market" / "Mercado libre" / "Mercado libre".
  - `league.market.listedBy` → "Listed by {name}" / "Listado por {name}" / "Listado por {name}".

**Tests:** mvn test → 152/152 green (145 prior + 7 new).
- `LeaderboardServiceTest` (4): squad value per user from current marketValue, 0 when no squad, points vs squad value are distinct concepts, forbidden for non-members.
- `UserMarketListingServiceTest` (3): listing carries seller + ~48h availableUntil + correct price; forbidden for non-owners; rejects non-positive prices.

**Frontend:** `pnpm exec tsc --noEmit` → 0 errors.

**Notes / out of scope:**
- Sellers cannot currently be prevented from bidding on their own listings - a simple guard can be added in `BidService.placeBid` if needed.
- Existing `MachineOffer` flow (system buy-back at ±10%) remains alongside the new user-listing flow; both are exposed to the FE.

### [2026-05-18] Correction ticket: 300M economy + valid lineup formations

**Slice 1 — Stronger initial squads**
- `LeagueRules`: `startingBudget` 250M → 300M; target band 200M-250M (was 150M-200M); standard composition changed from 2/4/6/3 to 2/5/5/3.
- `MarketValueConfig`: `startingUserBudget` and `futureMax` bumped to 300M.
- `InitialSquadAssignmentService` smarter pick algorithm:
  - `MAX_ATTEMPTS` 60 → 200.
  - First half of attempts use BIASED sampling: drawn from the top 65% of each position pool (sorted by `marketValue` DESC). Second half uses uniform random as a fallback.
  - When all random attempts miss the band, a GREEDY top-pick from each position is built and then `constrainBetween` swaps the most expensive picks down only as far as the lower target floor (200M), never below the band when possible.
  - Final fallback prefers whichever candidate (random "best" or greedy) has the LARGER squad value, so weak squads stop being accepted prematurely.
- Tests updated for the new defaults; new `biasesTowardHigherTierPlayersWhenBothAvailable` verifies the algorithm prefers the high-tier half of a mixed pool.

**Slice 2 — Valid lineup formations**
- New `Formation` value type listing the nine accepted shapes (1-3-4-3, 1-3-5-2, 1-3-6-1, 1-4-3-3, 1-4-4-2, 1-4-5-1, 1-5-2-3, 1-5-3-2, 1-5-4-1).
- `LineupValidator` now counts slot positions and rejects any shape not in the list - in addition to the existing player-ownership / slot-position / total-11 checks.
- `LineupController` exposes `GET /api/leagues/{id}/lineup/formations` returning the codes so the FE never duplicates the list.
- Frontend lineup page rebuilt:
  - Hardcoded 11-slot array replaced with `slotsFor(formationCode)` builder.
  - Formation `<select>` in the header (defaults to 1-4-4-2). Changing it remaps the lineup: players keep their slot when the new formation still has a same-position seat; players whose position is no longer represented in the new formation are removed from the lineup (they stay in the squad).
  - Inferred formation from a saved lineup on first load so the dropdown reflects reality.
- i18n key `league.lineup.formation` added in en/es/gl.
- Matchday frozen snapshot logic is unchanged - it reads whatever lineup is saved at lock time.

**Tests:** mvn test → 174/174 green (152 prior + 22 new).
- `FormationTest` (21 params):
  - All nine valid formations accepted.
  - Rejected: < 3 DEF, > 5 DEF, < 2 MID, > 6 MID, < 1 FWD, > 3 FWD, GK ≠ 1, total ≠ 11.
  - Listed codes match the spec.
- `InitialSquadAssignmentServiceTest.biasesTowardHigherTierPlayersWhenBothAvailable`: with a 50/50 high/low-tier pool the resulting squad value is ≥ 180M (close to the band).
- All other assignment-service tests updated for 300M budget / 2-5-5-3 composition.

**Frontend:** `pnpm exec tsc --noEmit` → 0 errors.

**Out of scope:**
- Scoring changes, dynamic market value, market bid resolution, scraping.
- The frontend's drag-to-reorder polish (current implementation already allows full slot picking via the modal).

### [2026-05-19] Lineup redesign — FantasyNations Lineup.html implementation

**Fetched** the Claude Design handoff bundle from the URL the user provided; unpacked to `/tmp/design/fantasy/`. The chat transcript and `fn-phone.jsx` / `fn-ui.jsx` / `fn-pitch.jsx` make the intent clear:
- Premium mobile-first lineup screen.
- Top: title + matchday selector (prev/next + dropdown) + Save button.
- Pitch: 3D-ish football field with stripes, halfway line, center circle, penalty boxes, stadium light + vignette.
- Trading-card "chromo" player cards on the pitch, with position chip + national team.
- Formation chip overlay (top-left of pitch) opening a bottom-sheet selector with all nine formations.
- Matchday bottom sheet listing every matchday with stage, status and my-points.
- Player picker bottom sheet with search, filtered by the slot position.
- Score mode (when viewing a FINISHED matchday): dark "Alineación de Jornada X" bar with total points at the top, and a small score badge on each player chromo.
- Save states: idle / saving / saved.

**Backend additions:**
- New DTO `MatchdayListItemDto { id, number, phase, status, lockAt, myTotalPoints }`.
- New `MatchdayController` exposes `GET /api/leagues/{leagueId}/matchdays` returning the seven matchdays enriched with the caller's `myTotalPoints` (null until aggregated).

**Frontend additions:**
- New API helpers in `matchdayApi.ts`: `getMatchdays`, `getMatchdayScore`, `getFormations`.
- Rewrote `frontend/src/app/leagues/[leagueId]/lineup/page.tsx` from scratch:
  - `Header` block — title + Save button + matchday selector with prev/next arrows + chevron dropdown.
  - `ScoreBar` — only in frozen score mode, with J-badge, "Lineup · Matchday N", stage, total points.
  - `Pitch` — SVG-based pitch with gradient grass, mowed stripes, halfway line, center circle, two penalty boxes, stadium light, vignette.
  - `PlayerChromo` — trading-card style cards: position chip, circular cutout image, name + national team, score badge in frozen mode (amber +N, rose -N, zinc 0).
  - Empty slot variant for unfilled positions.
  - `FormationGlyph` + `FormationDiagram` (mini-pitch with dots).
  - `BottomSheet` shell + three sheets: `FormationSheet`, `MatchdaySheet`, `PlayerPickerSheet`.
  - Score mode renders the frozen snapshot from `getMatchdayScore` (so a snapshotted player still shows even if they later left the squad). Edit mode renders the live lineup.
- i18n keys added in en/es/gl: `league.lineup.matchday`, `locksAt`, `frozenStatus`, `lockedStatus`.

**Verification:**
- `pnpm exec tsc --noEmit` → 0 errors.
- `mvn test` → 174/174 backend tests still green.

**Out of scope (kept for follow-up):**
- Desktop comp from the design (current page renders the mobile-first layout responsively but doesn't add the side rail).
- "Locked for current matchday" banner overlay (status is communicated via the matchday sheet for now).
- Animated drag-into-pitch reorder.
- Captain badge / multiplier (kept explicitly out of scope per earlier spec).
