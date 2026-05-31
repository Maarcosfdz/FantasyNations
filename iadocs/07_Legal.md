# Legal and Data Isolation

## Goal

All risky content must be isolated and easy to disable or replace.

Risky content includes:

- player images
- team logos
- stadium images
- official-looking branding
- scraped data
- provider-specific IDs
- external image URLs

The app must work even if these are disabled.

---

## Core rule

Do not hardcode risky content in pages or components.

Bad:

```tsx
<img src={player.imageUrl} />

Good:

const image = resolvePlayerImage(player);
<PlayerAvatar image={image} />
Required flags
PUBLIC_SAFE_MODE=false
SHOW_PLAYER_IMAGES=true
SHOW_TEAM_LOGOS=true
SHOW_STADIUM_IMAGES=false
ALLOW_SCRAPED_DATA=true
ALLOW_SCRAPED_IMAGES=false
DATA_SOURCE_MODE=scraping

When PUBLIC_SAFE_MODE=true, the app must hide:

player images
team logos
stadium images
official-looking branding

Use safe fallbacks instead.

Safe fallbacks

Player image fallback:

initials or generic avatar

Team logo fallback:

country code, country name or generic badge

Stadium image fallback:

stadium name only or hidden
Frontend structure

All asset logic goes here:

frontend/src/shared/assets/
  assetConfig.ts
  assetResolver.ts
  playerImageResolver.ts
  teamLogoResolver.ts
  fallbackAvatar.ts

Pages and components must not decide legal rules directly.

They must use the resolver.

Data source structure

All scraping/provider logic goes here:

backend/src/main/java/com/projectname/datasource/
  SportsDataSource.java
  ScrapingSportsDataSourceImpl.java
  MockSportsDataSourceImpl.java
  normalizer/
  dto/
  mapper/

The rest of the app must treat this as an internal API.

Allowed data source modes:

mock
csv
manual
scraping
provider
Data flow
Scraping / Provider / CSV
→ SportsDataSource
→ Normalizer
→ Internal database
→ App services
→ Frontend

Never do this:

Frontend → scraped website
Frontend → sports provider API
ScoringService → scraper
Provider IDs

Do not use external provider IDs as primary app IDs.

Store mappings separately.

Example:

ProviderMapping:
- providerName
- entityType
- internalEntityId
- externalEntityId
- sourceUrl
Scraping rules

Scraping must be replaceable.

Rules:

no scraping from frontend
no scraping in tests
no paid/external calls in tests
use mocks/fixtures
rate limit requests
cache when possible
fail safely
use last known data if available

Do not bypass paywalls, logins or technical restrictions.

Disclaimer

Use this disclaimer somewhere in the app:

This application is an independent private fantasy game created for recreational use among friends. It is not affiliated with, sponsored by, endorsed by, or associated with FIFA, any international tournament, national federations, clubs, players, stadiums or rights holders. Names, images, teams, venues and statistics are used only for informational, descriptive and recreational purposes. If any rights holder requests removal of content, the content can be reviewed and removed.
Review required

Update arevisar.md when changing:

scraping source
external provider
image source
team logo source
safe mode behavior
disclaimer
asset resolver behavior

Más corto todavía, para que lo lean siempre:

```md
# Legal/Data Short Rules

- No hardcoded player images, team logos, stadium images or provider URLs in components.
- Use `frontend/src/shared/assets/` resolvers for all risky assets.
- Use `PUBLIC_SAFE_MODE=true` to disable risky visuals.
- Use initials/country codes/generic avatars as fallbacks.
- Keep scraping/provider logic isolated in `datasource/`.
- App talks to `SportsDataSource`, never directly to scraper/provider.
- Never call scraper/provider from frontend.
- Never call scraper/provider in tests.
- Do not use external provider IDs as app primary IDs.
- Update `arevisar.md` for legal/data-source changes.