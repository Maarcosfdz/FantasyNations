import apiClient from "./apiClient";

export type MatchdayPhase = "GROUP" | "R16" | "QF" | "SF" | "FINAL";
export type MatchdayStatus = "SCHEDULED" | "LOCKED" | "FINISHED";

export interface MatchdayListItem {
  id: string;
  number: number;
  phase: MatchdayPhase;
  status: MatchdayStatus;
  lockAt: string | null;
  /** Caller's own total points for this matchday. Null if not aggregated yet. */
  myTotalPoints: number | null;
}

export async function getMatchdays(leagueId: string): Promise<MatchdayListItem[]> {
  const { data } = await apiClient.get<MatchdayListItem[]>(
    `/api/leagues/${leagueId}/matchdays`,
  );
  return data;
}

export interface MatchdayScoreResponse {
  matchdayId: string;
  matchdayNumber: number;
  leagueId: string;
  userId: string;
  totalPoints: number;
  reason: "OK" | "INCOMPLETE_LINEUP" | "NEGATIVE_BALANCE" | null;
  aggregatedAt: string | null;
  players: {
    playerId: string;
    playerName: string;
    nationalTeam: string;
    position: "GK" | "DEF" | "MID" | "FWD";
    positionSlot: string;
    imageRef: string | null;
    points: number;
    breakdown: Record<string, number>;
  }[];
}

export async function getMatchdayScore(
  leagueId: string,
  matchdayId: string,
): Promise<MatchdayScoreResponse> {
  const { data } = await apiClient.get<MatchdayScoreResponse>(
    `/api/leagues/${leagueId}/matchdays/${matchdayId}/score`,
  );
  return data;
}

export async function getFormations(leagueId: string): Promise<string[]> {
  const { data } = await apiClient.get<string[]>(
    `/api/leagues/${leagueId}/lineup/formations`,
  );
  return data;
}
