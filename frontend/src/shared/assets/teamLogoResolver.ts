import { PUBLIC_SAFE_MODE, SHOW_TEAM_LOGOS } from "./assetConfig";

export interface ResolvedTeamLogo {
  src: string | null;
  showLogo: boolean;
  countryCode: string;
}

export function resolveTeamLogo(
  logoRef: string | null | undefined,
  nationalTeam: string
): ResolvedTeamLogo {
  const countryCode = nationalTeam.slice(0, 3).toUpperCase();

  if (PUBLIC_SAFE_MODE || !SHOW_TEAM_LOGOS || !logoRef) {
    return { src: null, showLogo: false, countryCode };
  }

  return { src: logoRef, showLogo: true, countryCode };
}
