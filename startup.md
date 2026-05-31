# Startup Guide

## Prerequisites

- Docker + Docker Compose
- (Optional, for local dev) Java 21, Maven, Node.js 20, pnpm

---

## Option A — Docker (recommended)

### 1. Create your env file

```bash
cp .env.example .env
```

Edit `.env` and set at minimum:

```
POSTGRES_PASSWORD=yourpassword
JWT_SECRET=any-long-random-string-at-least-32-chars
```

### 2. Start all services

```bash
docker-compose up --build
```

This starts:
- **PostgreSQL** on port `5432`
- **Backend** (Spring Boot) on port `8080`
- **Frontend** (Next.js) on port `3000`

### 3. Check it works

| What | URL |
|---|---|
| Landing page | http://localhost:3000 |
| Backend health | http://localhost:8080/api/health |
| API docs (Swagger) | http://localhost:8080/api/docs/swagger-ui.html |

### 4. Stop

```bash
docker-compose down
# To also wipe the database:
docker-compose down -v
```

---

## Option B — Local dev (no Docker)

### 1. Start PostgreSQL

Make sure you have a local Postgres 15 instance running with a database called `fantasynations`.

```bash
# Quick way with Docker just for the DB:
docker run -d \
  --name fn-postgres \
  -e POSTGRES_DB=fantasynations \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:17-alpine
```

### 2. Start the backend

```bash
cd backend

# Copy and edit application config if needed (or use env vars):
# DATABASE_URL, JWT_SECRET must be set

./mvnw spring-boot:run
```

Backend runs on http://localhost:8080

### 3. Start the frontend

```bash
cd frontend

# Create local env file
cp .env.local.example .env.local
# Edit NEXT_PUBLIC_API_URL=http://localhost:8080

pnpm dev
```

Frontend runs on http://localhost:3000

---

## Verify the app

1. Open http://localhost:3000 — landing page with 3D trophy
2. Click **Login** → register a new account with email + password
3. After login → redirected to **My Leagues**
4. Click **Create league** → give it a name → enter the league
5. Go to **Market** tab → buy a player (costs money from your budget)
6. Go to **Team** tab → player appears in your squad
7. Go to **Lineup** tab → assign 11 players to slots → save
8. Go to **Ranking** tab → see league standings
9. Go to **Activity** tab → buy event appears in the feed

---

## World Cup player import

Real players come from `fant/players.json` and `fant/public/players/<team>/<player>.png`. This folder is the source of truth — drop new files in and re-run the app.

**Backend:** on startup the importer reads the JSON path from `app.players.json-path` (env var `PLAYERS_JSON_PATH`).

| Setup | What to do |
|---|---|
| Local dev | Default `../fant/players.json` resolves from `backend/` to the repo's `fant/` folder. Nothing to configure. |
| Docker | Mount `fant/` into the backend container (e.g. `-v $(pwd)/fant:/app/fant:ro`) and set `PLAYERS_JSON_PATH=/app/fant/players.json`. |
| Custom path | Set `PLAYERS_JSON_PATH=/absolute/path/to/players.json`. |

If the JSON file is missing the importer logs a clear error and the app keeps starting (with no players imported). Toggle the importer off entirely with `PLAYERS_IMPORT_ON_STARTUP=false`. The legacy fictional mock seeder is off by default; re-enable with `PLAYERS_MOCK_SEED_ON_STARTUP=true`.

**Frontend:** `pnpm dev` and `pnpm build` automatically copy `../fant/public/players` into `frontend/public/players` via `scripts/sync-players.mjs`. The target folder is gitignored. Run `pnpm sync-players` manually if you only want to refresh the assets.

---

## Safe mode (hides player images / team logos)

In `.env` or `.env.local`:

```
NEXT_PUBLIC_SAFE_MODE=true
```

Players show initials instead of images.

---

## Common issues

| Problem | Fix |
|---|---|
| Backend fails to start | Check `DATABASE_URL` in `.env` and that Postgres is running |
| Frontend can't reach API | Check `NEXT_PUBLIC_API_URL` matches backend host/port |
| `JWT_SECRET` error | Must be set and at least 32 characters long |
| Market is empty | Market auto-seeds from mock data on first startup — check backend logs |
