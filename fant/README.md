# World Cup 2022 Players JSON Toolkit

This toolkit generates a `players.json` file with players from the FIFA World Cup Qatar 2022 squads.

It is designed for the FantasyNations project.

## What it generates

Each player record follows this shape:

```json
{
  "name": "Lionel Messi",
  "national_team": "Argentina",
  "position": "FW",
  "club": "Paris Saint-Germain",
  "source_url": "https://en.wikipedia.org/wiki/2022_FIFA_World_Cup_squads",
  "wikipedia_url": "https://en.wikipedia.org/wiki/Lionel_Messi",
  "image_url": "https://upload.wikimedia.org/...",
  "image_source": "Wikimedia/Wikipedia",
  "license_note": "Check the image license before production use"
}
```

## Sources

- Player names, national teams, positions, and clubs are fetched from the Wikipedia page:
  `https://en.wikipedia.org/wiki/2022_FIFA_World_Cup_squads`
- Player images are resolved from each player's Wikipedia/Wikidata image when available.

## Important image/license note

Do not blindly use player photos in production.

Many player photos are copyrighted or have attribution requirements. This script stores image URLs and source metadata. Before using images commercially or publicly, check the license of each image on Wikimedia Commons or use a properly licensed sports data provider.

## How to run

Install dependencies:

```bash
pip install requests beautifulsoup4
```

Generate the JSON:

```bash
python generate_players_json.py
```

Output:

```text
players.json
```

## Why this approach

Instead of bundling downloaded player images, this creates a clean JSON with image URLs. That is safer for licensing and better for your app because the backend/frontend can decide whether to cache, proxy, or replace the images.
