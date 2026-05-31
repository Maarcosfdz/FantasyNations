# Code Architecture

## 1. Purpose

This document defines the code architecture, folder structure and naming conventions for the project.

Agents must follow this structure unless explicitly instructed otherwise.

The goal is to keep the codebase:

- easy to understand;
- easy to test;
- easy to maintain;
- consistent between frontend and backend;
- friendly for AI-assisted development.

---

## 2. General rules

## 2.1 Keep code close to where it is used

Code used by only one page, feature or module must stay inside that page, feature or module.

Code used by two or more places can be moved to a shared/global folder.

Do not create global abstractions too early.

---

## 2.2 Use descriptive names

Names must explain what the file, function, class, variable or constant does.

Avoid vague names:

```txt
data
item
thing
stuff
helper
manager
utils
temp
foo
bar
handler

Acceptable examples:

calculateFantasyPoints
validatePredictionDeadline
createLeagueInviteCode
getLeaderboardSnapshot
FantasyScoringServiceImpl
CreateLeagueRequestDto
2.3 Avoid unrelated refactors

When working on a task, only modify files related to that task.

Do not restructure folders unless the task requires it.

Do not move code globally unless it is reused by two or more modules/pages.

2.4 Keep files focused

A file should have one clear responsibility.

Avoid huge files with unrelated logic.

If a file becomes too large, split it by responsibility.

2.5 Prefer simple architecture

Avoid unnecessary patterns.

Do not add interfaces, factories, managers or abstractions unless they have a clear purpose.

Use interfaces when they help with:

testing;
replacing implementations;
external provider abstraction;
business boundary clarity.
3. Root structure

Recommended root structure:

project-root/
  AGENTS.md
  README.md
  arevisar.md
  .env.example
  .gitignore
  docker-compose.yml

  iadocs/
    response.md

  docs/
    00_OVERVIEW.md
    01_PRODUCT_SPEC.md
    02_GAME_RULES.md
    03_CODE_ARCHITECTURE.md
    04_DATA_MODEL.md
    05_DATA_PROVIDER.md
    06_LEGAL_AND_ASSETS.md
    07_AGENT_WORKFLOW.md
    08_TICKETS.md
    09_PERMISSIONS.md

  backend/
  frontend/

If the project uses a monorepo or a different existing structure, agents must adapt while keeping the same architectural principles.

4. Backend code architecture

The backend should use layered architecture.

Preferred backend technology:

Java + Spring Boot

If the project is already using a TypeScript backend, the same layered principles still apply.

4.1 Backend folder structure

Recommended Spring Boot structure:

backend/
  src/
    main/
      java/
        com/
          projectname/
            ProjectApplication.java

            config/
            security/
            controller/
            service/
            repository/
            domain/
            entity/
            dto/
            mapper/
            validation/
            exception/
            provider/
            scoring/
            leaderboard/
            util/

      resources/
        application.yml
        db/
          migration/

    test/
      java/
        com/
          projectname/
4.2 Backend package responsibilities
config/

Framework and application configuration.

Examples:

CorsConfig.java
OpenApiConfig.java
SecurityConfig.java

Do not place business logic here.

security/

Authentication, authorization, token handling and security helpers.

Examples:

JwtTokenService.java
AuthenticatedUserProvider.java
LeagueAuthorizationService.java

Rules:

no plain text passwords;
no token logging;
no secrets in code;
validate tokens properly;
centralize authorization helpers where useful.
controller/

HTTP/API boundary.

Responsibilities:

receive requests;
validate DTOs;
call services;
return response DTOs;
keep methods small.

Controllers must not contain business logic.

Example:

LeagueController.java
PredictionController.java
FantasySquadController.java
LeaderboardController.java
service/

Business logic.

Responsibilities:

league creation;
invite code logic;
squad validation;
prediction deadlines;
scoring orchestration;
leaderboard recalculation;
challenge logic.

If using interfaces, use this convention:

LeagueService.java
LeagueServiceImpl.java

Do not create interfaces unless they are useful.

repository/

Database access.

Examples:

LeagueRepository.java
PlayerRepository.java
PredictionRepository.java
LeaderboardSnapshotRepository.java

Repositories must not contain business rules.

Avoid raw SQL with string concatenated user input.

domain/

Domain objects, enums and business concepts that are not purely persistence entities.

Examples:

PlayerPosition.java
LeagueRole.java
PredictionResult.java
ScoringRule.java

Use this folder for core domain language.

entity/

Database persistence entities.

Examples:

LeagueEntity.java
PlayerEntity.java
MatchEntity.java
PredictionEntity.java
FantasySquadEntity.java

Entities should map database tables.

Do not expose entities directly through API responses.

Use DTOs.

dto/

Request and response DTOs.

All DTO names must end with:

Dto

Examples:

CreateLeagueRequestDto.java
LeagueResponseDto.java
JoinLeagueRequestDto.java
CreatePredictionRequestDto.java
LeaderboardEntryDto.java

Rules:

API input/output uses DTOs;
do not expose entities directly;
validate request DTOs;
keep DTOs simple.
mapper/

Mapping between:

entities;
domain models;
DTOs;
provider responses.

Examples:

LeagueMapper.java
PlayerMapper.java
PredictionMapper.java
ProviderPlayerMapper.java

Do not put business logic here.

validation/

Reusable validation logic.

Examples:

PredictionDeadlineValidator.java
SquadFormationValidator.java
InviteCodeValidator.java

Validation should be testable.

exception/

Custom exceptions and exception handlers.

Examples:

NotFoundException.java
ForbiddenException.java
InvalidInviteCodeException.java
GlobalExceptionHandler.java

User-facing errors must be safe and not leak internals.

provider/

External sports data provider integration.

Recommended structure:

provider/
  SportsDataProvider.java
  SportsDataProviderImpl.java

  dto/
    ProviderTeamDto.java
    ProviderPlayerDto.java
    ProviderMatchDto.java

  mapper/
    ProviderTeamMapper.java
    ProviderPlayerMapper.java
    ProviderMatchMapper.java

  exception/
    ProviderException.java

If multiple providers are added later:

provider/
  SportsDataProvider.java

  thesports/
    TheSportsProviderImpl.java
    dto/
    mapper/

  apifootball/
    ApiFootballProviderImpl.java
    dto/
    mapper/

  mock/
    MockSportsDataProviderImpl.java

Provider responses must not leak into services outside the provider layer.

scoring/

Fantasy, prediction and challenge scoring.

Examples:

FantasyScoringService.java
FantasyScoringServiceImpl.java
PredictionScoringService.java
PredictionScoringServiceImpl.java
ChallengeScoringService.java
ChallengeScoringServiceImpl.java
ScoringRules.java

Rules:

scoring must be deterministic;
scoring must be testable;
scoring must not call external APIs;
scoring must use normalized internal data.
leaderboard/

Leaderboard calculation and snapshots.

Examples:

LeaderboardService.java
LeaderboardServiceImpl.java
LeaderboardSnapshotService.java
LeaderboardSnapshotServiceImpl.java

Rules:

avoid recalculating full rankings on every request;
prefer stored leaderboard snapshots;
keep aggregation logic testable.
util/

Only for truly generic utilities.

Do not use this as a dumping ground.

Bad:

util/Helper.java
util/CommonUtils.java

Better:

util/DateTimeUtils.java
util/StringSlugUtils.java

If a utility belongs to a module, keep it inside that module instead.

5. Backend naming conventions
5.1 Classes

Use descriptive names.

Examples:

LeagueServiceImpl
CreateLeagueRequestDto
FantasyScoringServiceImpl
PredictionDeadlineValidator
LeaderboardSnapshotRepository
5.2 DTOs

Must end with:

Dto

Examples:

PlayerStatsDto
CreateSquadRequestDto
UpdatePredictionRequestDto
5.3 Interfaces and implementations

If an interface is used:

NameOfInterface.java
NameOfInterfaceImpl.java

Example:

SportsDataProvider.java
TheSportsDataProviderImpl.java
5.4 Repositories

Must end with:

Repository

Examples:

LeagueRepository
PredictionRepository
PlayerRepository
5.5 Controllers

Must end with:

Controller

Examples:

LeagueController
PredictionController
5.6 Services

Must end with:

Service

or:

ServiceImpl

Examples:

LeagueService
LeagueServiceImpl
5.7 Validators

Must end with:

Validator

Examples:

SquadFormationValidator
PredictionDeadlineValidator
5.8 Mappers

Must end with:

Mapper

Examples:

LeagueMapper
ProviderPlayerMapper
6. Frontend code architecture

The frontend should be organized by pages/features.

Page-specific code stays inside the page folder.

Shared code only becomes global when used by two or more pages/features.

6.1 Frontend folder structure

Recommended structure for Next.js App Router:

frontend/
  src/
    app/
      layout.tsx
      page.tsx
      globals.css

      leagues/
        page.tsx
        components/
          LeagueCard.tsx
          CreateLeagueForm.tsx
        hooks/
          useUserLeagues.ts
        utils/
          formatLeagueName.ts
        types/
          LeaguePageTypes.ts
        constants/
          leaguePageConstants.ts

      leagues/
        [leagueId]/
          page.tsx
          components/
            LeagueHeader.tsx
            LeagueMembersList.tsx
            LeagueLeaderboard.tsx
          hooks/
            useLeagueDetails.ts
          utils/
            formatLeaderboardPosition.ts
          types/
            LeagueDetailsPageTypes.ts
          constants/
            leagueDetailsConstants.ts

      predictions/
        page.tsx
        components/
          PredictionCard.tsx
          PredictionForm.tsx
        hooks/
          usePredictions.ts
        utils/
          formatPredictionDeadline.ts
        types/
          PredictionPageTypes.ts
        constants/
          predictionPageConstants.ts

      squad/
        page.tsx
        components/
          SquadPlayerCard.tsx
          SquadFormationView.tsx
        hooks/
          useFantasySquad.ts
        utils/
          validateSquadSelection.ts
        types/
          SquadPageTypes.ts
        constants/
          squadPageConstants.ts

    shared/
      components/
      hooks/
      utils/
      types/
      constants/
      api/
      auth/
      styles/
6.2 Page folder rules

Each page may have its own:

components/
hooks/
utils/
types/
constants/

Use these folders only when needed.

Do not create empty folders without reason.

6.3 Component rules

If a component is used by one page:

app/leagues/components/LeagueCard.tsx

If a component is used by two or more pages:

shared/components/LeagueCard.tsx

Do not move components to shared too early.

6.4 Hook rules

If a hook is used by one page:

app/predictions/hooks/usePredictions.ts

If a hook is used by two or more pages:

shared/hooks/useCurrentUser.ts

Hook names must start with:

use

Examples:

useLeagueDetails
useFantasySquad
usePredictionDeadline
6.5 Utility rules

Utilities should be close to their usage.

Page-specific:

app/squad/utils/validateSquadSelection.ts

Shared:

shared/utils/formatDateTime.ts

Avoid generic files like:

utils/helpers.ts
utils/common.ts

Prefer descriptive files:

formatMatchKickoffTime.ts
calculateRemainingTransfers.ts
buildPlayerDisplayName.ts
6.6 Type rules

Page-specific types:

app/leagues/types/LeaguePageTypes.ts

Shared types:

shared/types/LeagueTypes.ts
shared/types/PlayerTypes.ts
shared/types/MatchTypes.ts

DTO types should end with:

Dto

Examples:

CreateLeagueRequestDto
LeagueResponseDto
PredictionResponseDto
6.7 Constants rules

Constants should be descriptive.

Page-specific:

app/predictions/constants/predictionPageConstants.ts

Shared:

shared/constants/scoringConstants.ts

Examples:

MAX_PLAYERS_PER_SQUAD
INVITE_CODE_LENGTH
PREDICTION_LOCK_MINUTES_BEFORE_KICKOFF
DEFAULT_LEADERBOARD_PAGE_SIZE
6.8 API client rules

Frontend must call internal backend APIs only.

Recommended structure:

shared/api/
  apiClient.ts
  leagueApi.ts
  predictionApi.ts
  squadApi.ts
  leaderboardApi.ts

Rules:

do not call external sports providers from the frontend;
do not expose provider API keys;
centralize HTTP error handling;
validate or type API responses where practical.
6.9 Auth frontend rules

Recommended structure:

shared/auth/
  useCurrentUser.ts
  authClient.ts
  requireAuth.ts

Rules:

do not rely only on frontend auth checks;
backend must enforce authorization;
frontend may hide UI but backend must protect data.
7. Styling architecture

Use Tailwind CSS.

Global CSS should be minimal.

Allowed in global styles:

CSS reset if needed;
base font settings;
CSS variables;
body background;
very generic layout defaults.

Avoid global page-specific styles.

Page-specific styling should live inside page components.

Reusable design primitives may be shared only when reused.

Example shared components:

shared/components/Button.tsx
shared/components/Card.tsx
shared/components/Modal.tsx

Do not create a large design system before the UI direction is finalized.

8. Testing architecture

Tests should live close to the code being tested when possible.

Backend examples:

backend/src/test/java/com/projectname/service/LeagueServiceTest.java
backend/src/test/java/com/projectname/scoring/FantasyScoringServiceTest.java
backend/src/test/java/com/projectname/validation/SquadFormationValidatorTest.java

Frontend examples:

frontend/src/app/squad/utils/validateSquadSelection.test.ts
frontend/src/shared/utils/formatDateTime.test.ts
frontend/src/app/predictions/hooks/usePredictions.test.ts

Test important business logic.

Priority areas:

null inputs;
empty values;
invalid values;
SQL injection-sensitive inputs;
authorization;
invite codes;
prediction locking;
squad validation;
scoring rules;
provider normalization;
leaderboard ranking.

Do not call real paid APIs in tests.

Use mocks, fixtures or fake providers.

9. Environment and config structure

Use .env.example to document environment variables.

Never commit real .env files.

Example variables:

DATABASE_URL=
POSTGRES_USER=
POSTGRES_PASSWORD=
POSTGRES_DB=

JWT_SECRET=
TOKEN_EXPIRATION_SECONDS=

SPORTS_PROVIDER_NAME=
SPORTS_PROVIDER_API_KEY=

PUBLIC_SAFE_MODE=
SHOW_PLAYER_IMAGES=
SHOW_TEAM_LOGOS=

Config should be centralized.

Backend config:

backend/src/main/java/com/projectname/config/

Frontend config:

frontend/src/shared/config/
10. Docker structure

Recommended Docker files:

project-root/
  docker-compose.yml

  backend/
    Dockerfile

  frontend/
    Dockerfile

Docker Compose may include:

postgres
backend
frontend

Optional later:

redis
worker

Do not add extra infrastructure unless needed.

11. Provider integration structure

Provider code must be isolated.

Recommended backend structure:

backend/src/main/java/com/projectname/provider/
  SportsDataProvider.java
  SportsDataProviderImpl.java

  dto/
    ProviderTeamDto.java
    ProviderPlayerDto.java
    ProviderMatchDto.java
    ProviderEventDto.java

  mapper/
    ProviderTeamMapper.java
    ProviderPlayerMapper.java
    ProviderMatchMapper.java

  exception/
    ProviderException.java

If using TheSports later:

provider/
  SportsDataProvider.java

  thesports/
    TheSportsProviderImpl.java
    TheSportsClient.java
    dto/
    mapper/

Rules:

provider DTOs stay inside provider package;
provider DTOs must not be used by scoring or frontend;
map provider data into internal entities/models;
do not call provider APIs in tests.
12. Feature module examples
12.1 League feature

Backend:

controller/LeagueController.java
service/LeagueService.java
service/LeagueServiceImpl.java
repository/LeagueRepository.java
entity/LeagueEntity.java
dto/CreateLeagueRequestDto.java
dto/JoinLeagueRequestDto.java
dto/LeagueResponseDto.java
mapper/LeagueMapper.java
validation/InviteCodeValidator.java

Frontend:

app/leagues/
  page.tsx
  components/
    LeagueCard.tsx
    CreateLeagueForm.tsx
  hooks/
    useUserLeagues.ts
  utils/
    formatLeagueName.ts
  types/
    LeaguePageTypes.ts
  constants/
    leaguePageConstants.ts
12.2 Prediction feature

Backend:

controller/PredictionController.java
service/PredictionService.java
service/PredictionServiceImpl.java
repository/PredictionRepository.java
entity/PredictionEntity.java
dto/CreatePredictionRequestDto.java
dto/PredictionResponseDto.java
mapper/PredictionMapper.java
validation/PredictionDeadlineValidator.java

Frontend:

app/predictions/
  page.tsx
  components/
    PredictionCard.tsx
    PredictionForm.tsx
  hooks/
    usePredictions.ts
  utils/
    formatPredictionDeadline.ts
  types/
    PredictionPageTypes.ts
12.3 Scoring feature

Backend:

scoring/
  FantasyScoringService.java
  FantasyScoringServiceImpl.java
  PredictionScoringService.java
  PredictionScoringServiceImpl.java
  ChallengeScoringService.java
  ChallengeScoringServiceImpl.java
  ScoringRules.java

Tests:

FantasyScoringServiceTest.java
PredictionScoringServiceTest.java
ChallengeScoringServiceTest.java
13. When to create shared/global code

Move code to shared/global only when:

it is used by two or more pages/modules;
it is a true design primitive;
it is a true domain type;
it is a true API utility;
it avoids real duplication.

Do not move code to shared/global because it "might be useful later".

14. Anti-patterns to avoid

Avoid:

components/
  EverythingComponent.tsx

utils/
  helpers.ts

service/
  AppService.java

controller/
  MainController.java

dto/
  DataDto.java

types/
  types.ts

Avoid:

huge files;
vague names;
business logic in controllers;
business logic in UI components;
database logic in services when repositories exist;
provider DTOs leaking into domain logic;
untested scoring logic;
global components used by only one page;
excessive abstractions;
adding dependencies without reason.
15. Agent-specific rules

Agents must follow this architecture.

If the existing project structure differs, adapt carefully and document the reason in:

iadocs/response.md

If a task requires deviating from this architecture, document it in:

arevisar.md

Do not ask for clarification unless blocked.

When ambiguous, choose the simplest MVP-compatible option, document the assumption and continue.