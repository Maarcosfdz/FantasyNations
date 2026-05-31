# AGENTS.md

## Project

Fantasy Nations is a private fantasy football web app for friend groups themed around national team tournaments.

Users create private leagues, join by invite code, buy players from a daily market, build squads, select lineups, earn points and compete in rankings.

---

## Communication rules

Agents must not write long explanations in chat.

When finishing a task, the chat response must be extremely short:

```txt
Done. Updated iadocs/response.md and arevisar.md if needed.
```

Document all decisions and assumptions in:

- `iadocs/response.md`
- `arevisar.md` (for items needing review)

---

## Permissions

Agents have full permission to work inside this repository:

- read, create, edit, delete, move, rename files and folders
- run project commands
- install dependencies
- run tests, linters, builds
- use Docker

Do NOT modify files outside this repository.

---

## Tech stack

- Frontend: Next.js (App Router), TypeScript, Tailwind CSS, shadcn/ui, pnpm
- Backend: Java 21, Spring Boot 3.x
- Database: PostgreSQL 15 + Flyway
- Auth: Spring Security + JWT + Google OAuth2
- 3D: three.js + @react-three/fiber + @react-three/drei
- Animations: motion (framer-motion)
- Forms: react-hook-form + zod
- Server state: @tanstack/react-query
- Client state: zustand
- Icons: lucide-react
- Infrastructure: Docker + docker-compose
- Package manager: pnpm

---

## Key rules

- No commits, no push.
- Use pnpm for frontend.
- Mobile-first, web-first.
- Keep scraping/data source isolated in `backend/.../datasource/`.
- Keep risky assets isolated via `frontend/src/shared/assets/` resolvers.
- Never hardcode player image URLs or team logo URLs in components — use resolvers.
- Use env flags: `PUBLIC_SAFE_MODE`, `SHOW_PLAYER_IMAGES`, `SHOW_TEAM_LOGOS`.
- No plain text passwords.
- No secrets in code.
- Data source starts as `DATA_SOURCE_MODE=mock`.
- 3D trophy must be generic — not a copy of any official trophy.
- Document decisions in `iadocs/response.md`.
- Document review items in `arevisar.md`.

---

## Architecture reference

See `iadocs/04_Architecture.MD` for full architecture details.

Short summary:

- Backend: layered (controller → service → repository → entity).
- Frontend: feature-folder structure per page.
- Shared code only when used by 2+ pages.
- All scoring must be deterministic and testable.
- Asset resolver centralizes all image/logo logic.
- Scoring must not call external APIs.
