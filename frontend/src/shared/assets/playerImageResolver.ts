import { PUBLIC_SAFE_MODE, SHOW_PLAYER_IMAGES } from "./assetConfig";

export interface ResolvedPlayerImage {
  src: string | null;
  showImage: boolean;
  initials: string;
}

export function resolvePlayerImage(
  imageRef: string | null | undefined,
  playerName: string
): ResolvedPlayerImage {
  const initials = playerName
    .split(" ")
    .map((p) => p[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  if (PUBLIC_SAFE_MODE || !SHOW_PLAYER_IMAGES || !imageRef) {
    return { src: null, showImage: false, initials };
  }

  // Local public asset paths emitted by the World Cup importer
  // (e.g. "/players/argentina/franco-armani.png") are served verbatim
  // by Next.js from frontend/public/. Anything else is treated as an
  // opaque ref a future CDN resolver could map.
  const src = imageRef.startsWith("/players/") ? imageRef : imageRef;
  return { src, showImage: true, initials };
}
