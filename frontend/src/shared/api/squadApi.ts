import apiClient from "./apiClient";

export interface SquadPlayer {
  squadPlayerId: string;
  playerId: string;
  playerName: string;
  nationalTeam: string;
  position: "GK" | "DEF" | "MID" | "FWD";
  imageRef: string | null;
  currentValue: number;
  releaseClause: number;
  protectedUntil: string | null;
  acquiredAt: string;
}

export async function getSquad(leagueId: string): Promise<SquadPlayer[]> {
  const { data } = await apiClient.get<SquadPlayer[]>(`/api/leagues/${leagueId}/squad`);
  return data;
}

export async function payReleaseClause(leagueId: string, playerId: string): Promise<void> {
  await apiClient.post(`/api/leagues/${leagueId}/squad/clause/${playerId}`);
}

export async function updateReleaseClause(
  leagueId: string,
  playerId: string,
  releaseClause: number
): Promise<SquadPlayer> {
  const { data } = await apiClient.patch<SquadPlayer>(
    `/api/leagues/${leagueId}/squad/${playerId}/clause`,
    { releaseClause }
  );
  return data;
}
