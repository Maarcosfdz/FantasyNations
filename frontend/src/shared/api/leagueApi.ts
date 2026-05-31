import apiClient from "./apiClient";

export interface LeagueRules {
  /** Total budget each user gets at league join: players + remaining money. Not user-configurable. */
  startingBudget: number;
  moneyPerPoint: number;
  releaseClauseProtectionHours: number;
  marketRefreshIntervalHours: number;
  marketPlayersCount: number;
  maxPlayersPerSquad: number;
  minLineupPlayers: number;
  formationRulesEnabled: boolean;
  /** Initial squad shape, not user-configurable. */
  initialSquadSize: number;
  initialSquadGk: number;
  initialSquadDef: number;
  initialSquadMid: number;
  initialSquadFwd: number;
}

export interface League {
  id: string;
  name: string;
  inviteCode: string;
  ownerId: string;
  ownerNickname: string;
  memberCount: number;
  rules: LeagueRules;
  createdAt: string;
}

export async function getUserLeagues(): Promise<League[]> {
  const { data } = await apiClient.get<League[]>("/api/leagues");
  return data;
}

export async function getLeague(leagueId: string): Promise<League> {
  const { data } = await apiClient.get<League>(`/api/leagues/${leagueId}`);
  return data;
}

export async function createLeague(payload: {
  name: string;
  moneyPerPoint?: number;
}): Promise<League> {
  const { data } = await apiClient.post<League>("/api/leagues", payload);
  return data;
}

export async function joinLeague(inviteCode: string): Promise<League> {
  const { data } = await apiClient.post<League>("/api/leagues/join", { inviteCode });
  return data;
}

export interface LeagueMembership {
  userId: string;
  leagueId: string;
  money: number;
  role: "OWNER" | "MEMBER";
  joinedAt: string;
}

export async function getMyMembership(leagueId: string): Promise<LeagueMembership> {
  const { data } = await apiClient.get<LeagueMembership>(`/api/leagues/${leagueId}/me`);
  return data;
}

export async function updateLeagueSettings(
  leagueId: string,
  rules: LeagueRules
): Promise<League> {
  const { data } = await apiClient.put<League>(
    `/api/leagues/${leagueId}/settings`,
    rules
  );
  return data;
}
