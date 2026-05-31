import apiClient from "./apiClient";

export interface ActivityEntry {
  id: string;
  eventType: string;
  userNickname: string;
  payload: Record<string, unknown>;
  createdAt: string;
}

export async function getActivity(leagueId: string): Promise<ActivityEntry[]> {
  const { data } = await apiClient.get<ActivityEntry[]>(`/api/leagues/${leagueId}/activity`);
  return data;
}
