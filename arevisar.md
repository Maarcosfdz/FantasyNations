# arevisar.md — Items Needing Review

This file tracks decisions, deviations, assumptions and items that need human review.

---

## Open items

### [2026-05-17] Data source mode set to MOCK

**Decision:** `DATA_SOURCE_MODE=mock` for MVP. MockSportsDataSourceImpl seeds 30+ players from fictional national teams.

**Review:** When ready to use real data, switch to `DATA_SOURCE_MODE=csv` or `DATA_SOURCE_MODE=provider` and update the implementation in `backend/.../datasource/`.

---

### [2026-05-17] Scraping deferred

**Decision:** Python scraping skeleton is not implemented in MVP. The data source interface is ready to add it later.

**Review:** Add `data-source/python/` folder and implement `ScrapingSportsDataSourceImpl` when scraping is needed.

Candidate sources to evaluate (see `iadocs/11_data_source.md`): Sofascore, FBref, Transfermarkt, Flashscore. Use APIs (API-Football, Sportmonks) as a more stable alternative.

---

### [2026-05-17] Google OAuth2 — client credentials required

**Decision:** Google OAuth2 flow is implemented but requires real credentials to function.

**Review:** Add `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to `.env` before testing OAuth. Register app at Google Cloud Console.

---

### [2026-05-17] Email/password reset — email sender not configured

**Decision:** Forgot password flow is implemented but requires an SMTP sender (e.g., SendGrid, Mailgun, Gmail SMTP).

**Review:** Configure `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD` in `.env` and enable `spring.mail.*` settings.

---

### [2026-05-17] Player images use image_ref (not URL)

**Decision:** Players store an `image_ref` string (not a direct URL). The asset resolver maps refs to actual URLs or fallbacks based on env flags.

**Review:** When real player images are available, update `playerImageResolver.ts` to map refs to CDN URLs or local assets.

---

### [2026-05-17] 3D trophy is a parametric generic cup

**Decision:** The 3D trophy is built from three.js primitives (cylinder, torus, lathe geometry). It does not copy any official trophy.

**Review:** A custom `.glb` model can replace it later via `useGLTF`. Place model in `frontend/public/models/trophy.glb` and update `TrophyCanvas.tsx`.

---

### [2026-05-17] Lineup validation is basic (11 players, user owns them)

**Decision:** No formation validation in MVP. Users can place any 11 owned players.

**Review:** Enable formation validation by implementing `SquadFormationValidator` when formation rules are needed.

---

### [2026-05-17] Hardcoded coefficients in market/clause flows

**Decision:** `MarketServiceImpl.buyPlayer` sets `releaseClause = price * 1.5` and `SquadServiceImpl.payReleaseClause` returns `clause * 0.8` to the seller. These coefficients are not configurable league rules.

**Review:** Per `03_Game_RULES.md` ("market value change rules" + "money earned per point"), these factors should live in `LeagueRules` (e.g. `initialClauseMultiplier`, `clauseResaleRatio`) so leagues can tune them. Out of scope for the current Slice A but flagged.

---

### [2026-05-17] iadocs/05_Model.md and iadocs/06_Agent_Workflow.md are empty

**Decision:** Both files contain a single non-content line. The architecture (`04_Architecture.MD`) does cover the data layout indirectly, but the explicit data-model and agent-workflow docs are missing.

**Review:** Author these two docs before adding new entities or changing the agent workflow.

---

### [2026-05-17] GSAP + Lenis installed (Slice D)

**Decision:** Installed `gsap@3.15.0` and `lenis@1.3.23`. GSAP is currently used only by `HeroSection.tsx` for a scroll-scrubbed parallax (registered through `gsap.context()` + `ScrollTrigger`, cleaned up on unmount). Lenis is wired through `landing/SmoothScrollProvider.tsx`, gated behind `prefers-reduced-motion: reduce`, scoped to the landing route only.

**Review:** If Lenis stays scoped to landing and GSAP usage doesn't grow beyond the hero, the cost is fine. If they're not used elsewhere within 1–2 sprints, consider removing GSAP and replacing the parallax with a `motion` `useScroll` + `useTransform` (already installed). Don't extend GSAP usage to authenticated pages without re-checking bundle size first.

---

### [2026-05-17] Money per point algorithm is simplified

**Decision:** `moneyEarned = totalPoints * moneyPerPoint`. Applied manually or by admin trigger for now.

**Review:** Automate scoring recalculation after match data is imported. Schedule via Spring `@Scheduled` when data source is live.
