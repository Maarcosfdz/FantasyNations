#!/usr/bin/env node
/**
 * Mirrors ../fant/public/players into ./public/players so Next.js can serve
 * player images from the canonical /players/<team>/<player>.png URL.
 *
 * The fant/ folder is the source of truth (easy to drop new images into).
 * public/players is a derived directory and gitignored.
 *
 * Idempotent: safe to run on every predev/prebuild.
 */
import { existsSync, mkdirSync, cpSync, rmSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SOURCE = resolve(__dirname, "..", "..", "fant", "public", "players");
const TARGET = resolve(__dirname, "..", "public", "players");

if (!existsSync(SOURCE)) {
  console.warn(
    `[sync-players] source folder not found: ${SOURCE}\n` +
      `[sync-players] skipping. Player images will fall back to placeholders.`
  );
  process.exit(0);
}

if (existsSync(TARGET)) {
  rmSync(TARGET, { recursive: true, force: true });
}
mkdirSync(TARGET, { recursive: true });
cpSync(SOURCE, TARGET, { recursive: true });

console.log(`[sync-players] synced ${SOURCE} -> ${TARGET}`);
