"use client";

import { use, useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getMarket, placeBid, type MarketPlayer } from "@/shared/api/marketApi";
import { getMyMembership } from "@/shared/api/leagueApi";
import { resolvePlayerImage } from "@/shared/assets/playerImageResolver";
import { getAvatarColor } from "@/shared/assets/fallbackAvatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Wallet, Clock, Lock, User, Globe } from "lucide-react";
import { toast } from "sonner";
import { useT } from "@/shared/i18n/I18nProvider";

interface Props {
  params: Promise<{ leagueId: string }>;
}

const positionColors: Record<string, string> = {
  GK: "bg-amber-100 text-amber-800",
  DEF: "bg-sky-100 text-sky-800",
  MID: "bg-emerald-100 text-emerald-800",
  FWD: "bg-rose-100 text-rose-800",
};

function formatRemaining(target: Date | null, refreshingLabel: string): string {
  if (!target) return "—";
  const diff = target.getTime() - Date.now();
  if (diff <= 0) return refreshingLabel;
  const totalMinutes = Math.floor(diff / 60_000);
  const h = Math.floor(totalMinutes / 60);
  const m = totalMinutes % 60;
  return `${String(h).padStart(2, "0")}h ${String(m).padStart(2, "0")}m`;
}

const moneyCompact = (v: number) =>
  new Intl.NumberFormat("en", {
    style: "currency",
    currency: "EUR",
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(v);

const moneyFull = (v: number) =>
  new Intl.NumberFormat("en", {
    style: "currency",
    currency: "EUR",
    maximumFractionDigits: 0,
  }).format(v);

export default function MarketPage({ params }: Props) {
  const { leagueId } = use(params);
  const queryClient = useQueryClient();
  const t = useT();

  const { data: marketResponse, isLoading: marketLoading } = useQuery({
    queryKey: ["market", leagueId],
    queryFn: () => getMarket(leagueId),
  });
  const market = marketResponse?.players;

  const { data: me } = useQuery({
    queryKey: ["membership", leagueId],
    queryFn: () => getMyMembership(leagueId),
  });

  // Live countdown tick every 30s
  const [, setTick] = useState(0);
  useEffect(() => {
    const id = setInterval(() => setTick((t) => t + 1), 30_000);
    return () => clearInterval(id);
  }, []);

  const nextRefresh = marketResponse?.nextRefreshAt
    ? new Date(marketResponse.nextRefreshAt)
    : null;

  // Auto-refetch shortly after the cycle closes so resolved listings disappear.
  useEffect(() => {
    if (!nextRefresh) return;
    const ms = nextRefresh.getTime() - Date.now();
    if (ms <= 0) return;
    const id = setTimeout(
      () => queryClient.invalidateQueries({ queryKey: ["market", leagueId] }),
      ms + 5_000,
    );
    return () => clearTimeout(id);
  }, [nextRefresh, leagueId, queryClient]);

  return (
    <div className="p-4 max-w-lg mx-auto">
      {/* Summary bar */}
      <div className="mb-3 grid grid-cols-2 gap-2">
        <div className="bg-white border border-zinc-200 shadow-sm rounded-xl px-3 py-2.5 flex items-center gap-2">
          <Wallet className="h-4 w-4 text-emerald-600 flex-shrink-0" />
          <div className="min-w-0">
            <div className="text-[10px] uppercase tracking-wide text-zinc-500">
              {t("league.market.yourMoney")}
            </div>
            <div className="text-sm font-bold text-zinc-900 truncate">
              {me ? moneyFull(me.money) : "—"}
            </div>
          </div>
        </div>
        <div className="bg-white border border-zinc-200 shadow-sm rounded-xl px-3 py-2.5 flex items-center gap-2">
          <Clock className="h-4 w-4 text-amber-500 flex-shrink-0" />
          <div className="min-w-0">
            <div className="text-[10px] uppercase tracking-wide text-zinc-500">
              {t("league.market.nextRefresh")}
            </div>
            <div className="text-sm font-bold text-zinc-900 tabular-nums">
              {formatRemaining(nextRefresh, t("league.market.refreshing"))}
            </div>
          </div>
        </div>
      </div>

      <h2 className="text-xl font-bold text-zinc-900 mb-2">{t("league.market.title")}</h2>

      <div className="mb-4 bg-blue-50 border border-blue-200 text-blue-900 rounded-xl px-3 py-2 flex items-start gap-2">
        <Lock className="h-4 w-4 mt-0.5 flex-shrink-0" />
        <p className="text-xs leading-relaxed">{t("league.market.secretNotice")}</p>
      </div>

      {marketLoading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-400" />
        </div>
      ) : market && market.length > 0 ? (
        <div className="space-y-3">
          {market.map((mp) => (
            <ListingCard
              key={mp.id}
              listing={mp}
              userMoney={me?.money}
              onAfterBid={() => {
                queryClient.invalidateQueries({ queryKey: ["market", leagueId] });
              }}
              leagueId={leagueId}
            />
          ))}
        </div>
      ) : marketResponse?.reason === "NO_PLAYERS_IN_POOL" ? (
        <div className="text-center py-12 text-zinc-500">
          <p>{t("league.market.noPool")}</p>
          <p className="text-xs mt-1">{t("league.market.noPoolHint")}</p>
        </div>
      ) : (
        <div className="text-center py-12 text-zinc-500">
          <p>{t("league.market.empty")}</p>
          <p className="text-xs mt-1">{t("league.market.emptyHint")}</p>
        </div>
      )}
    </div>
  );
}

function ListingCard({
  listing,
  userMoney,
  onAfterBid,
  leagueId,
}: {
  listing: MarketPlayer;
  userMoney?: number;
  onAfterBid: () => void;
  leagueId: string;
}) {
  const t = useT();
  const img = resolvePlayerImage(listing.imageRef, listing.playerName);
  const [bidText, setBidText] = useState<string>(
    listing.ownBidAmount != null ? String(listing.ownBidAmount) : String(listing.price),
  );
  const [submitting, setSubmitting] = useState(false);

  const parsed = Number(bidText);
  const validNumber = Number.isFinite(parsed) && parsed > 0;
  const cantAfford = userMoney != null && validNumber && parsed > userMoney;

  async function submit() {
    if (!validNumber) {
      toast.error(t("league.market.bidMustBePositive"));
      return;
    }
    if (cantAfford) {
      toast.error(t("league.market.notEnoughMoney"));
      return;
    }
    if (!confirm(
      t("league.market.bidConfirm", { name: listing.playerName, amount: moneyFull(parsed) }),
    )) return;

    setSubmitting(true);
    try {
      await placeBid(leagueId, listing.id, parsed);
      toast.success(
        listing.ownBidAmount != null
          ? t("league.market.bidUpdated")
          : t("league.market.bidSubmitted"),
      );
      onAfterBid();
    } catch {
      toast.error(t("league.market.bidFailed"));
    } finally {
      setSubmitting(false);
    }
  }

  const buttonLabel =
    listing.ownBidAmount != null ? t("league.market.bidUpdate") : t("league.market.bidSubmit");

  return (
    <div className="bg-white border border-zinc-200 shadow-sm rounded-xl p-4">
      <div className="flex items-center gap-3 mb-3">
        <div
          className="w-11 h-11 rounded-full flex items-center justify-center text-sm font-bold text-white flex-shrink-0 overflow-hidden"
          style={{ background: img.showImage ? undefined : getAvatarColor(listing.playerName) }}
        >
          {img.showImage ? (
            <img src={img.src!} alt={listing.playerName} className="w-11 h-11 rounded-full object-cover" />
          ) : (
            img.initials
          )}
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2 mb-0.5">
            <span
              className={`text-[10px] font-semibold uppercase tracking-wide px-1.5 py-0.5 rounded ${positionColors[listing.position]}`}
            >
              {listing.position}
            </span>
            <span className="font-semibold text-zinc-900 truncate">{listing.playerName}</span>
          </div>
          <span className="text-xs text-zinc-500">{listing.nationalTeam}</span>
          <div className="mt-0.5">
            {listing.sellerNickname ? (
              <span className="inline-flex items-center gap-1 text-[10px] font-medium px-1.5 py-0.5 rounded bg-purple-50 text-purple-700">
                <User className="h-3 w-3" />
                {t("league.market.listedBy", { name: listing.sellerNickname })}
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 text-[10px] font-medium px-1.5 py-0.5 rounded bg-zinc-100 text-zinc-600">
                <Globe className="h-3 w-3" />
                {t("league.market.freeMarket")}
              </span>
            )}
          </div>
        </div>
        <div
          className="text-right flex-shrink-0 text-sm font-bold text-amber-500 tabular-nums"
          title={t("league.market.bidStartingPrice", { amount: moneyFull(listing.price) })}
        >
          {moneyCompact(listing.price)}
        </div>
      </div>

      {listing.ownBidAmount != null && (
        <div className="text-xs text-blue-700 mb-2">
          {t("league.market.bidPending", { amount: moneyFull(listing.ownBidAmount) })}
        </div>
      )}

      <div className="flex items-center gap-2">
        <label className="text-xs text-zinc-600 sr-only" htmlFor={`bid-${listing.id}`}>
          {t("league.market.bidLabel")}
        </label>
        <Input
          id={`bid-${listing.id}`}
          type="number"
          inputMode="numeric"
          min={1}
          placeholder={t("league.market.bidPlaceholder")}
          value={bidText}
          onChange={(e) => setBidText(e.target.value)}
          className="bg-white border-zinc-300 tabular-nums h-8 text-sm"
        />
        <Button
          size="sm"
          onClick={submit}
          disabled={submitting || !validNumber || cantAfford}
          className="bg-blue-600 hover:bg-blue-700 disabled:opacity-40 text-xs h-8 whitespace-nowrap"
        >
          {buttonLabel}
        </Button>
      </div>
    </div>
  );
}
