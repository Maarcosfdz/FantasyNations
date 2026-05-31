Player value algorithm

Player values should change over time.

Initial MVP can use a simple formula.

Possible factors:

recent points;
average points;
player popularity;
market demand;
goals/assists;
starting probability;
real tournament performance.

The algorithm must be isolated so it can be replaced later.

Activity log

The league activity log records important events.

Examples:

user joined league;
league created;
player bought from market;
release clause paid;
player sold;
market refreshed;
lineup changed;
league rules changed.

Activity should show the last month by default.

Important implementation rule

Do not hardcode rules inside UI components.

Rules should live in backend/domain services and configurable league settings.


---

# `docs/10_UI_SPEC.md`

```md
# UI Spec

## Goal

The UI should feel modern, visual and fun.

The app is web-first but must work well on mobile.

The first design priority is mobile, then tablet, then desktop.

---

## Landing page

The landing page is the public presentation of Fantasy Nations.

Sections:

1. Hero
2. Description
3. About
4. Login/Register modal

---

## Hero section

The hero should include:

- Fantasy Nations title;
- 3D generic football trophy/cup;
- football stadium background layer;
- strong visual presentation;
- scroll behavior to more information.

The 3D object must be generic and not an official trophy copy.

---

## About section

The About section should include:

- short app description;
- GitHub link;
- LinkedIn link.

Links:

- GitHub: https://github.com/Maarcosfdz
- LinkedIn: https://www.linkedin.com/in/marcos-romay-82b16036a/

---

## Login modal

Login/register should appear in a modal or popup.

It should support:

- Google OAuth;
- email/password login;
- register;
- forgot password;
- password reset.

---

## Authenticated layout

After login, the user sees the My Leagues screen.

The authenticated area should have:

- a menu button at the top-left;
- user profile/avatar access;
- responsive layout;
- clean navigation.

---

## My Leagues screen

Must show:

- list of leagues;
- create league button;
- join league button.

---

## League layout

Inside a league, use a bottom navigation/footer selector.

Tabs:

```txt
Team | Lineup | Ranking | Market | Activity

Ranking is the central/default tab.

Team tab

Shows the user's full squad.

Should include:

player cards;
player images;
player names;
values;
release clauses;
actions.
Lineup tab

Shows a football pitch background.

Users select the starting 11 visually.

Should include:

player placement;
position display;
player image or fallback;
easy mobile interaction.
Ranking tab

Shows league ranking.

Should include:

position;
nickname;
avatar;
total points;
optional breakdown.
Market tab

Shows the daily market.

Should include:

available players;
image;
name;
team;
position;
price;
buy action.
Activity tab

Shows recent league activity.

Default period:

last month.
User profile

Users should be able to manage:

nickname;
profile image;
email;
password/reset flow if applicable.
Theme

The UI must support:

light mode;
dark mode;
easy future theme changes.

Use semantic tokens instead of hardcoded colors where possible.


---

# `docs/05_DATA_PROVIDER.md`

```md
# Data Provider and Scraping

## Goal

The app needs football data, player data, images, values and match statistics.

Data collection must be isolated so the app can change from scraping to API, CSV or manual data later.

---

## Data source modes

Supported modes should be:

```txt
mock
csv
manual
scraping
provider

Controlled by:

DATA_SOURCE_MODE=scraping
Main rule

The application must not depend directly on scraping code.

Correct flow:

Scraper / API / CSV
→ Data source module
→ Normalizer
→ Internal database
→ App services
→ Frontend

Wrong flow:

Frontend → scraped website
ScoringService → scraper
MarketService → raw scraping result
Python scraping

Scraping may be implemented using Python scripts.

Recommended folder:

data-source/
  python/
    scrapers/
    normalizers/
    exporters/
    fixtures/
    tests/

The Python scripts should output normalized data in a stable format such as:

JSON;
CSV;
database import format.

The backend should consume normalized output, not raw HTML.

Backend data source interface

Backend should expose a provider-like interface.

Example:

public interface SportsDataSource {
    List<ExternalTeamDto> getTeams();
    List<ExternalPlayerDto> getPlayers();
    List<ExternalMatchDto> getMatches();
    List<ExternalMatchEventDto> getMatchEvents(String externalMatchId);
    List<ExternalPlayerStatsDto> getPlayerStats(String externalMatchId);
}

Possible implementations:

ScrapingSportsDataSourceImpl
CsvSportsDataSourceImpl
ManualSportsDataSourceImpl
MockSportsDataSourceImpl
ProviderSportsDataSourceImpl

The rest of the app should depend on SportsDataSource, not on a concrete scraper.

Normalization

External data must be normalized before use.

Normalize:

teams;
players;
matches;
stadiums;
player images;
team names;
player positions;
match events;
player stats;
market values.

Provider-specific IDs must not be used as primary app IDs.

Provider mapping

Store external IDs separately.

Example:

ProviderMapping
- providerName
- entityType
- internalEntityId
- externalEntityId
- sourceUrl
Scraping rules

Scraping must be replaceable and safe.

Rules:

no scraping from frontend;
no scraping in tests;
no paid/external calls in tests;
use fixtures/mocks;
cache when possible;
rate limit requests;
fail safely;
do not bypass paywalls, logins or technical restrictions.
Betting websites

The initial idea is to scrape some data from sports betting websites.

This must be isolated and easy to replace.

Do not spread betting-site-specific logic across the app.

If a source breaks or must be removed, only the data-source module should need changes.

Review required

Update arevisar.md when changing:

scraping source;
data source mode;
external provider;
player image source;
team image source;
normalization logic;
data sync behavior.

---

# `docs/08_TICKETS.md`

```md
# Tickets

## T01 - Project documentation setup

Create or update:

- AGENTS.md
- iadocs/response.md
- arevisar.md
- docs/01_PRODUCT_SPEC.md
- docs/02_GAME_RULES.md
- docs/03_CODE_ARCHITECTURE.md
- docs/05_DATA_PROVIDER.md
- docs/06_LEGAL_AND_ASSETS.md
- docs/10_UI_SPEC.md

No app features required in this ticket.

---

## T02 - Initial frontend landing page

Implement the public landing page.

Requirements:

- Fantasy Nations title;
- hero section;
- stadium-style background;
- placeholder for 3D generic trophy/cup;
- description section;
- About section;
- GitHub link;
- LinkedIn link;
- login/register modal trigger;
- responsive layout.

Do not use official tournament branding.

---

## T03 - Auth modal UI

Implement login/register modal UI.

Requirements:

- Google OAuth button placeholder;
- email input;
- password input;
- register mode;
- login mode;
- forgot password link;
- basic validation;
- responsive design.

Authentication backend can be mocked if not ready.

---

## T04 - Auth backend

Implement authentication.

Requirements:

- email/password register;
- email/password login;
- logout;
- forgot password/reset flow if supported by chosen auth system;
- Google OAuth if configured;
- user profile with nickname and email.

Security:

- no plain text passwords;
- no secrets in frontend;
- validate inputs.

---

## T05 - My Leagues screen

Implement authenticated My Leagues screen.

Requirements:

- list joined leagues;
- create league button;
- join league button;
- empty state;
- responsive design.

---

## T06 - Create and join league

Implement league creation and joining.

Requirements:

- create league with name;
- generate invite code;
- join league by invite code;
- store owner/member roles;
- show league in My Leagues.

---

## T07 - League settings

Implement configurable league settings.

Initial settings:

- starting money;
- money per point;
- release clause protection time;
- market refresh interval;
- number of market players.

---

## T08 - League layout with bottom navigation

Implement league internal layout.

Tabs:

- Team
- Lineup
- Ranking
- Market
- Activity

Ranking should be the central/default tab.

---

## T09 - Player model and manual player import

Create player model and a way to add/import players.

Requirements:

- player name;
- image URL or asset reference;
- national team;
- position;
- base value;
- current value;
- active/inactive.

Keep it easy to add players manually.

---

## T10 - Market basic version

Implement daily market basic version.

Requirements:

- show market players;
- refresh every 24 hours;
- buy player;
- check user money;
- add player to squad;
- create activity event.

---

## T11 - Squad and team tab

Implement user squad/team tab.

Requirements:

- show owned players;
- show player image/fallback;
- show value;
- show release clause;
- allow release clause increase if implemented.

---

## T12 - Lineup tab

Implement lineup selection.

Requirements:

- football pitch background;
- select starting 11;
- validate owned players;
- save lineup;
- responsive mobile-first UI.

---

## T13 - Release clause system

Implement release clause purchases.

Requirements:

- allow buying another user's player by clause;
- transfer player to buyer;
- update money;
- apply protection time;
- create activity event.

---

## T14 - Ranking

Implement league ranking.

Requirements:

- show members;
- show total points;
- show position;
- show nickname/avatar;
- prepare structure for point breakdown.

---

## T15 - Activity log

Implement league activity.

Requirements:

- store events;
- show last month;
- activity types for buys, clauses, joins, market refresh and rule changes.

---

## T16 - Data source module

Create isolated data-source structure.

Requirements:

- support mock/csv/manual/scraping/provider modes;
- create interface;
- create mock implementation;
- create normalizer placeholder;
- do not call external websites yet unless explicitly requested.

---

## T17 - Python scraping skeleton

Create Python scraping skeleton.

Requirements:

- scraper folder;
- fixture folder;
- normalizer folder;
- exporter to JSON/CSV;
- tests with fixtures;
- no real scraping in tests.

---

## T18 - Asset resolver and safe mode

Implement asset resolver.

Requirements:

- PUBLIC_SAFE_MODE;
- SHOW_PLAYER_IMAGES;
- SHOW_TEAM_LOGOS;
- player image fallback;
- team logo fallback;
- no hardcoded risky URLs in components.

---

## T19 - Player value algorithm v1

Implement simple player value algorithm.

Requirements:

- deterministic;
- testable;
- isolated from UI;
- based on points/performance placeholders;
- easy to replace later.

---

## T20 - Money earned by points

Implement money earned based on points.

Requirements:

- league setting moneyPerPoint;
- apply money updates after points calculation;
- test edge cases.

---

## T21 - User profile

Implement user profile settings.

Requirements:

- change nickname;
- upload/change avatar if storage exists;
- show email;
- password reset/change flow if supported.
Añadir a docs/AGENT_CONTEXT_SHORT.md
# Short Agent Context

Fantasy Nations is a private fantasy football app for friend leagues.

Core screens:

- public landing page;
- login/register modal;
- My Leagues;
- League with bottom tabs: Team, Lineup, Ranking, Market, Activity.

Core mechanics:

- users create/join leagues;
- users buy players from daily market;
- users can pay release clauses for players owned by others;
- users select a starting 11;
- rankings are based on points;
- money can be earned from points;
- player values and release clauses change over time.

Important rules:

- web-first and mobile-friendly;
- use pnpm;
- no commits;
- work on develop;
- keep risky assets isolated;
- keep scraping/data source isolated;
- do not hardcode player image URLs in components;
- use safe mode flags for images/logos;
- document important decisions in iadocs/response.md;
- document review items in arevisar.md.