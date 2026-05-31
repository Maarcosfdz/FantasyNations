import apiClient from "./apiClient";

export interface RankingEntry {
  rank: number;
  userId: string;
  nickname: string;
  avatarUrl: string | null;
  totalPoints: number;
  /** Sum of current market value of every player this user owns in the league. */
  squadValue: number;
}

export async function getRanking(leagueId: string): Promise<RankingEntry[]> {
  const { data } = await apiClient.get<RankingEntry[]>(`/api/leagues/${leagueId}/ranking`);
  return data;
}
