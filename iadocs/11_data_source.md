Fuentes recomendadas
1. Para empezar gratis / manual

Estas te valen para preparar datos base, pero no para live data serio:

Kaggle
Wikipedia
Transfermarkt como referencia visual/manual
FBref como referencia estadística/manual
Sofascore/FotMob como referencia visual/manual

Kaggle tiene datasets históricos del Mundial 2022 con jugadores, estadísticas e incluso imágenes, útiles para prototipar con datos falsos o antiguos, aunque no son datos del Mundial 2026 ni necesariamente tienen licencia para producción pública.

Para MVP, puedes hacer:

players.csv
teams.csv
matches.csv
player_images.csv

Y meter tú los datos importantes.

2. APIs baratas o con free tier

Estas son mejores que scraping si quieres algo más estable:

football-data.org
API-Football
Sportmonks
TheSports

football-data.org tiene plan gratuito, fixtures, schedules, league tables y límites de 10 requests/minuto en free registrado; puede servir para calendario/resultados si la cobertura te encaja.

API-Football tiene plan gratis limitado y planes desde $19/mes con endpoints de fixtures, eventos, alineaciones, jugadores, estadísticas y odds. Para un proyecto amateur puede ser de las opciones más razonables de precio.

Sportmonks tiene trial/free plan y planes desde unos €29/mes, con cobertura de World Cup, livescores, fixtures, squads y estadísticas.

3. Webs para scraping o referencia

Aquí iría con cuidado. Yo las pondría en docs como candidatas, no como compromiso.

Sofascore
FotMob
FBref
Transfermarkt
Flashscore
OddsPortal
Bet365 / casas de apuestas
Wikipedia

Pero para apuestas, ojo: muchas webs tienen medidas anti-scraping, términos restrictivos o cambian mucho. No diseñaría el sistema dependiendo de una casa de apuestas concreta.

Para odds/valores de mercado, mejor sería:

API de odds si algún día la necesitas
o
algoritmo propio basado en puntos y popularidad

No necesitas odds reales para tu fantasy. Puedes crear tu propio valor de jugador.

Qué pondría en docs/05_DATA_PROVIDER.md

Algo corto:

# Data Provider

## Goal

The app must support different data sources without changing business logic.

Initial data may come from:

- manual CSV files;
- mock JSON files;
- Python scraping scripts;
- external football APIs.

The app must not depend directly on a specific website.

---

## Data source modes

Supported modes:

```txt
mock
csv
manual
scraping
provider

Configured by:

DATA_SOURCE_MODE=mock
Candidate sources
Manual / prototype
CSV files
JSON fixtures
Kaggle historical datasets
Wikipedia manual data

Use these for development and tests.

APIs
football-data.org
API-Football
Sportmonks
TheSports

Use APIs if scraping becomes unstable or risky.

Scraping/reference candidates
Sofascore
FotMob
FBref
Transfermarkt
Flashscore
OddsPortal
Wikipedia

These sources must only be used through the isolated data-source layer.

Do not call them from frontend code.

Do not spread website-specific selectors or URLs across the app.

Main rule

The rest of the app talks only to:

SportsDataSource

Possible implementations:

MockSportsDataSource
CsvSportsDataSource
ManualSportsDataSource
ScrapingSportsDataSource
ApiFootballSportsDataSource
SportmonksSportsDataSource
TheSportsSportsDataSource
Data flow
Source
→ SportsDataSource
→ Normalizer
→ Internal database
→ App services
→ Frontend

Never:

Frontend → external website
Frontend → provider API
ScoringService → scraper
MarketService → raw scraped response
Python scraping folder

If scraping is used, keep it isolated:

data-source/
  python/
    scrapers/
    normalizers/
    exporters/
    fixtures/
    tests/

Python scripts should output normalized JSON or CSV.

The backend should consume normalized files or an internal endpoint, not raw HTML.

Suggested first approach

Start with:

DATA_SOURCE_MODE=mock

Then add:

DATA_SOURCE_MODE=csv

Then later:

DATA_SOURCE_MODE=scraping

or:

DATA_SOURCE_MODE=provider
Testing

Tests must use:

mock data
fixtures
local CSV/JSON

Tests must not call real external websites or paid APIs.

Review required

Update arevisar.md when changing:

scraping source;
provider;
data source mode;
image source;
normalization logic;
player value algorithm input.

## Mi consejo real

Para no perderte:

```txt
1. Empieza con CSV/manual.
2. Mete 20 jugadores fake/reales a mano.
3. Haz mercado, ligas, alineación y clausulazos.
4. Cuando el juego funcione, conectas datos reales.

Porque si empiezas por scraping, te puedes atascar antes de tener juego.

Para tu caso, el orden bueno sería:

Mock JSON → CSV manual → API barata → scraping solo si hace falta

I want scrapping