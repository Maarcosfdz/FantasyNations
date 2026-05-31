#!/usr/bin/env python3
"""
Scrape FIFA Qatar 2022 squad pages using the real player-card DOM.

This version specifically takes the player image from:

  player-badge-card_playerImageContainer
    image_imgContainer

and avoids the flag image from:

  player-badge-card_playerFlag

Target examples:
https://www.fifa.com/es/tournaments/mens/worldcup/qatar2022/teams
https://www.fifa.com/es/tournaments/mens/worldcup/qatar2022/teams/spain/squad

Install:
    pip install playwright requests pillow
    python -m playwright install chromium

Test Spain only:
    python .\scrape_fifa_qatar2022_players_v5.py --teams spain --headful

Run all:
    python .\scrape_fifa_qatar2022_players_v5.py

Optional background removal:
    pip install rembg
    python .\scrape_fifa_qatar2022_players_v5.py --remove-bg
"""

from __future__ import annotations

import argparse
import json
import re
import time
import unicodedata
from io import BytesIO
from pathlib import Path
from typing import Any
from urllib.parse import urljoin

import requests
from PIL import Image
from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeoutError


BASE_URL = "https://www.fifa.com"
TEAMS_URL = "https://www.fifa.com/es/tournaments/mens/worldcup/qatar2022/teams"

TEAM_SLUGS = [
    "argentina", "australia", "belgium", "brazil", "cameroon", "canada",
    "costa-rica", "croatia", "denmark", "ecuador", "england", "france",
    "germany", "ghana", "iran", "japan", "korea-republic", "mexico",
    "morocco", "netherlands", "poland", "portugal", "qatar", "saudi-arabia",
    "senegal", "serbia", "spain", "switzerland", "tunisia", "united-states",
    "uruguay", "wales",
]

TEAM_NAME_FROM_SLUG = {
    "argentina": "Argentina",
    "australia": "Australia",
    "belgium": "Belgium",
    "brazil": "Brazil",
    "cameroon": "Cameroon",
    "canada": "Canada",
    "costa-rica": "Costa Rica",
    "croatia": "Croatia",
    "denmark": "Denmark",
    "ecuador": "Ecuador",
    "england": "England",
    "france": "France",
    "germany": "Germany",
    "ghana": "Ghana",
    "iran": "Iran",
    "japan": "Japan",
    "korea-republic": "Korea Republic",
    "mexico": "Mexico",
    "morocco": "Morocco",
    "netherlands": "Netherlands",
    "poland": "Poland",
    "portugal": "Portugal",
    "qatar": "Qatar",
    "saudi-arabia": "Saudi Arabia",
    "senegal": "Senegal",
    "serbia": "Serbia",
    "spain": "Spain",
    "switzerland": "Switzerland",
    "tunisia": "Tunisia",
    "united-states": "United States",
    "uruguay": "Uruguay",
    "wales": "Wales",
}

POSITION_MAP = {
    "portero": "GK",
    "arquero": "GK",
    "goalkeeper": "GK",
    "defensa": "DF",
    "defender": "DF",
    "centrocampista": "MF",
    "mediocampista": "MF",
    "midfielder": "MF",
    "delantero": "FW",
    "forward": "FW",
    "striker": "FW",
}

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/126.0 Safari/537.36"
    ),
    "Accept": "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
}


def normalize(value: str) -> str:
    value = value.lower().strip()
    value = unicodedata.normalize("NFKD", value)
    value = "".join(ch for ch in value if not unicodedata.combining(ch))
    value = value.replace("ø", "o").replace("đ", "d").replace("ð", "d")
    value = value.replace("ł", "l").replace("ß", "ss").replace("ı", "i")
    value = re.sub(r"[^a-z0-9]+", " ", value)
    return re.sub(r"\s+", " ", value).strip()


def slugify(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "-", normalize(value)).strip("-")


def clean_player_name(value: str) -> str:
    value = value.replace("\xa0", " ")
    value = re.sub(r"\s+", " ", value).strip()
    value = re.sub(r"^\d+\s*", "", value)
    parts = value.split()
    if not parts:
        return value
    return " ".join(part if not part.isupper() else part.capitalize() for part in parts)


def map_position(value: str | None) -> str | None:
    if not value:
        return None

    norm = normalize(value)
    for key, mapped in POSITION_MAP.items():
        if key in norm:
            return mapped

    if norm in {"gk", "df", "mf", "fw"}:
        return norm.upper()

    return value.strip() or None


def safe_goto(page, url: str) -> None:
    print(f"Opening: {url}", flush=True)
    page.goto(url, wait_until="domcontentloaded", timeout=60000)
    page.wait_for_timeout(2500)


def accept_cookies(page) -> None:
    for text in ["Aceptar todo", "Aceptar", "Accept all", "I Accept", "Allow all"]:
        try:
            locator = page.get_by_text(text, exact=False).first
            if locator.count() > 0 and locator.is_visible(timeout=800):
                locator.click(timeout=2000)
                page.wait_for_timeout(1000)
                return
        except Exception:
            pass


def scroll_until_cards_loaded(page, min_cards: int = 20, max_rounds: int = 20) -> None:
    last_count = -1
    stable_rounds = 0

    for _ in range(max_rounds):
        count = page.locator('div[class*="player-badge-card_badgeCard"]').count()

        if count >= min_cards and count == last_count:
            stable_rounds += 1
        else:
            stable_rounds = 0

        if stable_rounds >= 2:
            break

        last_count = count
        page.mouse.wheel(0, 1200)
        page.wait_for_timeout(700)

    page.mouse.wheel(0, -99999)
    page.wait_for_timeout(500)


def collect_team_slugs_from_fifa(page) -> list[str]:
    safe_goto(page, TEAMS_URL)
    accept_cookies(page)

    for _ in range(10):
        page.mouse.wheel(0, 1000)
        page.wait_for_timeout(400)

    hrefs = page.evaluate(
        """
        () => Array.from(document.querySelectorAll('a[href]'))
          .map(a => a.getAttribute('href') || '')
          .filter(h => h.includes('/tournaments/mens/worldcup/qatar2022/teams/'))
        """
    )

    slugs: list[str] = []
    seen: set[str] = set()

    for href in hrefs:
        match = re.search(r"/teams/([^/?#]+)", href)
        if not match:
            continue

        slug = match.group(1)
        if slug in seen:
            continue

        if slug in TEAM_NAME_FROM_SLUG:
            seen.add(slug)
            slugs.append(slug)

    if slugs:
        print(f"Found {len(slugs)} team slugs from FIFA teams page", flush=True)
        return slugs

    print("Could not read team slugs from FIFA page. Using fallback list.", flush=True)
    return TEAM_SLUGS[:]


def extract_players_from_squad_page(page, slug: str, debug_dir: Path) -> list[dict[str, Any]]:
    team_name = TEAM_NAME_FROM_SLUG.get(slug, slug.replace("-", " ").title())
    squad_url = f"{BASE_URL}/es/tournaments/mens/worldcup/qatar2022/teams/{slug}/squad"

    safe_goto(page, squad_url)
    accept_cookies(page)

    try:
        page.wait_for_selector('div[class*="player-badge-card_badgeCard"]', timeout=25000)
    except PlaywrightTimeoutError:
        print("  No player card selector found yet; scrolling and retrying...", flush=True)

    scroll_until_cards_loaded(page, min_cards=20)

    cards_count = page.locator('div[class*="player-badge-card_badgeCard"]').count()
    print(f"  Found {cards_count} player-card DOM nodes", flush=True)

    raw_players = page.evaluate(
        """
        () => {
          const cards = Array.from(document.querySelectorAll('div[class*="player-badge-card_badgeCard"]'));

          const extractUrl = (value) => {
            if (!value || value === 'none') return null;
            const m = String(value).match(/url\\(["']?(.*?)["']?\\)/);
            return m ? m[1] : null;
          };

          const firstFromSrcset = (srcset) => {
            if (!srcset) return null;
            const candidates = srcset
              .split(',')
              .map(x => x.trim().split(/\\s+/)[0])
              .filter(Boolean);
            return candidates[candidates.length - 1] || candidates[0] || null;
          };

          const getFromElement = (el) => {
            if (!el) return null;

            // Direct image / picture / source inside the player-image area.
            const directImg = el.querySelector('img');
            if (directImg) {
              const src = directImg.currentSrc || directImg.src || directImg.getAttribute('src');
              if (src && !src.startsWith('data:')) return src;

              const srcset = directImg.getAttribute('srcset');
              const fromSrcset = firstFromSrcset(srcset);
              if (fromSrcset && !fromSrcset.startsWith('data:')) return fromSrcset;
            }

            const source = el.querySelector('picture source[srcset], source[srcset]');
            if (source) {
              const fromSource = firstFromSrcset(source.getAttribute('srcset'));
              if (fromSource && !fromSource.startsWith('data:')) return fromSource;
            }

            // Background image on the player-image container or children.
            const candidates = [el, ...Array.from(el.querySelectorAll('*'))];

            for (const node of candidates) {
              const attrStyle = node.getAttribute && node.getAttribute('style');
              const attrUrl = extractUrl(attrStyle);
              if (attrUrl && !attrUrl.startsWith('data:')) return attrUrl;

              const style = window.getComputedStyle(node);
              const bg = extractUrl(style.backgroundImage);
              if (bg && !bg.startsWith('data:')) return bg;

              const before = window.getComputedStyle(node, '::before');
              const beforeBg = extractUrl(before.backgroundImage);
              if (beforeBg && !beforeBg.startsWith('data:')) return beforeBg;

              const after = window.getComputedStyle(node, '::after');
              const afterBg = extractUrl(after.backgroundImage);
              if (afterBg && !afterBg.startsWith('data:')) return afterBg;
            }

            return null;
          };

          return cards.map(card => {
            const nameEl =
              card.querySelector('[class*="player-badge-card_playerName"] [title]') ||
              card.querySelector('[class*="playerName"] [title]') ||
              card.querySelector('[class*="player-badge-card_playerName"] span') ||
              card.querySelector('[class*="playerName"] span') ||
              card.querySelector('[class*="player-badge-card_playerName"]') ||
              card.querySelector('[class*="playerName"]');

            const positionEl =
              card.querySelector('[class*="player-badge-card_playerPosition"]') ||
              card.querySelector('[class*="playerPosition"]') ||
              card.querySelector('[class*="position"]');

            // IMPORTANT:
            // Only search inside playerImageContainer.
            // Do NOT search playerFlag, because that gives the nation flag.
            const playerImageContainer =
              card.querySelector('[class*="player-badge-card_playerImageContainer"]') ||
              card.querySelector('[class*="playerImageContainer"]');

            const imageImgContainer =
              playerImageContainer && (
                playerImageContainer.querySelector('[class*="image_imgContainer"]') ||
                playerImageContainer.querySelector('[class*="player-badge-card_playerImage"]') ||
                playerImageContainer
              );

            const name =
              (nameEl && (nameEl.getAttribute('title') || nameEl.textContent || '').trim()) || '';

            const position =
              (positionEl && (positionEl.getAttribute('title') || positionEl.textContent || '').trim()) || '';

            return {
              name,
              position,
              imageUrl: getFromElement(imageImgContainer),
              text: (card.innerText || '').trim(),
              playerImageHtml: playerImageContainer ? playerImageContainer.outerHTML.slice(0, 2000) : null
            };
          });
        }
        """
    )

    players: list[dict[str, Any]] = []
    seen: set[str] = set()

    for item in raw_players:
        name = clean_player_name(item.get("name") or "")

        if not name:
            text_lines = [
                line.strip()
                for line in (item.get("text") or "").splitlines()
                if line.strip()
            ]
            if text_lines:
                name = clean_player_name(text_lines[0])

        if not name or len(name) < 3:
            continue

        key = normalize(name)
        if key in seen:
            continue

        seen.add(key)

        img = item.get("imageUrl")
        img = urljoin(BASE_URL, img) if img else None

        players.append({
            "name": name,
            "national_team": team_name,
            "position": map_position(item.get("position")),
            "_remote_image_url": img,
        })

    found_images = sum(1 for p in players if p.get("_remote_image_url"))
    print(f"  {team_name}: extracted {len(players)} players, {found_images} player images", flush=True)

    if len(players) < 20 or found_images < len(players):
        debug_dir.mkdir(parents=True, exist_ok=True)
        debug_path = debug_dir / f"{slug}-squad-debug.json"
        debug_path.write_text(
            json.dumps({
                "url": squad_url,
                "cards_count": cards_count,
                "raw_count": len(raw_players),
                "players_count": len(players),
                "images_found": found_images,
                "raw_players": raw_players,
                "players": players,
            }, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        print(f"  Debug saved: {debug_path}", flush=True)

    return players


def download_image_as_png(remote_url: str, destination: Path, remove_bg: bool, referer: str) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)

    if destination.exists():
        return

    headers = dict(HEADERS)
    headers["Referer"] = referer

    response = requests.get(remote_url, headers=headers, timeout=45)
    response.raise_for_status()

    raw = response.content

    if remove_bg:
        try:
            from rembg import remove
            destination.write_bytes(remove(raw))
            return
        except Exception as exc:
            print(f"    Background removal failed: {exc}", flush=True)

    with Image.open(BytesIO(raw)) as img:
        img.convert("RGBA").save(destination, "PNG")


def save_images_and_build_json(players: list[dict[str, Any]], public_output: Path, public_url_prefix: str, remove_bg: bool, delay: float) -> list[dict[str, Any]]:
    public_url_prefix = "/" + public_url_prefix.strip("/")
    output: list[dict[str, Any]] = []

    for idx, player in enumerate(players, start=1):
        team = player["national_team"]
        name = player["name"]
        remote = player.get("_remote_image_url")

        team_slug = slugify(team)
        player_slug = slugify(name)
        destination = public_output / team_slug / f"{player_slug}.png"

        print(f"[{idx}/{len(players)}] {team} - {name}", flush=True)

        image_url = None

        if remote:
            try:
                download_image_as_png(
                    remote_url=remote,
                    destination=destination,
                    remove_bg=remove_bg,
                    referer=TEAMS_URL,
                )
                image_url = f"{public_url_prefix}/{team_slug}/{player_slug}.png"
                time.sleep(delay)
            except Exception as exc:
                print(f"    Image download failed: {exc}", flush=True)

        output.append({
            "name": name,
            "national_team": team,
            "position": player.get("position"),
            "image_url": image_url,
        })

    return output


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="players.json")
    parser.add_argument("--public-output", default="public/players")
    parser.add_argument("--public-url-prefix", default="/players")
    parser.add_argument("--debug-dir", default="debug_fifa")
    parser.add_argument("--headful", action="store_true")
    parser.add_argument("--remove-bg", action="store_true")
    parser.add_argument("--delay", type=float, default=0.25)
    parser.add_argument("--max-teams", type=int, default=None)
    parser.add_argument(
        "--teams",
        default=None,
        help='Comma-separated team slugs, e.g. "spain,argentina,france"',
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()

    with sync_playwright() as p:
        browser = p.chromium.launch(
            headless=not args.headful,
            args=[
                "--disable-blink-features=AutomationControlled",
                "--disable-dev-shm-usage",
            ],
        )

        context = browser.new_context(
            viewport={"width": 1440, "height": 1400},
            locale="es-ES",
            user_agent=HEADERS["User-Agent"],
        )

        page = context.new_page()
        page.set_default_timeout(15000)

        if args.teams:
            team_slugs = [slug.strip() for slug in args.teams.split(",") if slug.strip()]
        else:
            team_slugs = collect_team_slugs_from_fifa(page)

        if args.max_teams:
            team_slugs = team_slugs[:args.max_teams]

        all_players: list[dict[str, Any]] = []

        for index, slug in enumerate(team_slugs, start=1):
            print(f"\n=== Team {index}/{len(team_slugs)}: {slug} ===", flush=True)
            try:
                all_players.extend(
                    extract_players_from_squad_page(
                        page=page,
                        slug=slug,
                        debug_dir=Path(args.debug_dir),
                    )
                )
            except KeyboardInterrupt:
                raise
            except Exception as exc:
                print(f"  Failed team {slug}: {exc}", flush=True)

        browser.close()

    deduped: list[dict[str, Any]] = []
    seen: set[str] = set()

    for player in all_players:
        key = f"{normalize(player['national_team'])}|{normalize(player['name'])}"
        if key in seen:
            continue
        seen.add(key)
        deduped.append(player)

    print(f"\nTotal extracted players: {len(deduped)}", flush=True)

    final_players = save_images_and_build_json(
        players=deduped,
        public_output=Path(args.public_output),
        public_url_prefix=args.public_url_prefix,
        remove_bg=args.remove_bg,
        delay=args.delay,
    )

    payload = {
        "competition": "FIFA World Cup Qatar 2022",
        "source": TEAMS_URL,
        "total_players": len(final_players),
        "players": final_players,
    }

    Path(args.output).write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    print(f"\nGenerated {args.output} with {len(final_players)} players", flush=True)


if __name__ == "__main__":
    main()
