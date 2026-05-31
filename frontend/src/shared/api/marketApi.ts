import apiClient from "./apiClient";

export interface MarketPlayer {
  id: string;
  playerId: string;
  playerName: string;
  nationalTeam: string;
  position: "GK" | "DEF" | "MID" | "FWD";
  imageRef: string | null;
  price: number;
  currentValue: number;
  availableUntil: string;
  /** Amount of the caller's own bid on this listing, or null if not bid yet. */
  ownBidAmount: number | null;
  /** NULL = free-market / system listing. Non-null = user-listed (48h). */
  sellerUserId: string | null;
  /** Display nickname of the seller (null for free market). */
  sellerNickname: string | null;
}

export type MarketReason = "NO_PLAYERS_IN_POOL" | "NOT_ENOUGH_PLAYERS" | null;

export interface MarketResponse {
  available: boolean;
  nextRefreshAt: string | null;
  players: MarketPlayer[];
  reason: MarketReason;
}

export type BidStatus = "SUBMITTED" | "WON" | "LOST" | "REJECTED_NO_FUNDS";

export interface BidResponse {
  id: string;
  marketPlayerId: string;
  amount: number;
  status: BidStatus;
  submittedAt: string;
}

export type MachineOfferStatus = "PENDING" | "ACCEPTED" | "EXPIRED";

export interface MachineOffer {
  id: string;
  squadPlayerId: string;
  amount: number;
  status: MachineOfferStatus;
  expiresAt: string;
}

export interface QuickSellResult {
  squadPlayerId: string;
  amountCredited: number;
}

export async function getMarket(leagueId: string): Promise<MarketResponse> {
  const { data } = await apiClient.get<MarketResponse>(`/api/leagues/${leagueId}/market`);
  return data;
}

export async function placeBid(
  leagueId: string,
  listingId: string,
  amount: number,
): Promise<BidResponse> {
  const { data } = await apiClient.post<BidResponse>(
    `/api/leagues/${leagueId}/market/listings/${listingId}/bid`,
    { amount },
  );
  return data;
}

export async function listSquadPlayerForSale(
  leagueId: string,
  squadPlayerId: string,
): Promise<MachineOffer> {
  const { data } = await apiClient.post<MachineOffer>(
    `/api/leagues/${leagueId}/squad/${squadPlayerId}/list-for-sale`,
  );
  return data;
}

export async function acceptMachineOffer(
  leagueId: string,
  offerId: string,
): Promise<MachineOffer> {
  const { data } = await apiClient.post<MachineOffer>(
    `/api/leagues/${leagueId}/offers/${offerId}/accept`,
  );
  return data;
}

export async function listOnMarket(
  leagueId: string,
  squadPlayerId: string,
  askingPrice: number,
): Promise<MarketPlayer> {
  const { data } = await apiClient.post<MarketPlayer>(
    `/api/leagues/${leagueId}/squad/${squadPlayerId}/list-on-market`,
    { askingPrice },
  );
  return data;
}

export async function quickSell(
  leagueId: string,
  squadPlayerId: string,
): Promise<QuickSellResult> {
  const { data } = await apiClient.post<QuickSellResult>(
    `/api/leagues/${leagueId}/squad/${squadPlayerId}/quick-sell`,
  );
  return data;
}
