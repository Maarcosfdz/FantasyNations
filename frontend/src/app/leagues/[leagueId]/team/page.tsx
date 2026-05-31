"use client";

import { use, useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getSquad,
  updateReleaseClause,
  type SquadPlayer,
} from "@/shared/api/squadApi";
import { getMyMembership } from "@/shared/api/leagueApi";
import {
  listSquadPlayerForSale,
  acceptMachineOffer,
  quickSell,
  type MachineOffer,
} from "@/shared/api/marketApi";
import { resolvePlayerImage } from "@/shared/assets/playerImageResolver";
import { getAvatarColor } from "@/shared/assets/fallbackAvatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Wallet, Pencil, X, Zap, Tag } from "lucide-react";
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

export default function TeamPage({ params }: Props) {
  const { leagueId } = use(params);
  const queryClient = useQueryClient();
  const t = useT();

  const { data: squad, isLoading } = useQuery({
    queryKey: ["squad", leagueId],
    queryFn: () => getSquad(leagueId),
  });

  const { data: me } = useQuery({
    queryKey: ["membership", leagueId],
    queryFn: () => getMyMembership(leagueId),
  });

  const [clausePlayer, setClausePlayer] = useState<SquadPlayer | null>(null);
  const [pendingOffer, setPendingOffer] = useState<{ offer: MachineOffer; playerName: string } | null>(null);

  function invalidateSquad() {
    queryClient.invalidateQueries({ queryKey: ["squad", leagueId] });
    queryClient.invalidateQueries({ queryKey: ["activity", leagueId] });
    queryClient.invalidateQueries({ queryKey: ["membership", leagueId] });
  }

  async function handleListForSale(sp: SquadPlayer) {
    if (!confirm(t("league.team.listForSaleConfirm", { name: sp.playerName }))) return;
    try {
      const offer = await listSquadPlayerForSale(leagueId, sp.squadPlayerId);
      setPendingOffer({ offer, playerName: sp.playerName });
    } catch {
      toast.error(t("league.team.listingFailed"));
    }
  }

  async function handleAcceptOffer() {
    if (!pendingOffer) return;
    try {
      await acceptMachineOffer(leagueId, pendingOffer.offer.id);
      toast.success(t("league.team.offerAccepted", {
        name: pendingOffer.playerName,
        amount: moneyFull(pendingOffer.offer.amount),
      }));
      setPendingOffer(null);
      invalidateSquad();
    } catch {
      toast.error(t("league.team.offerFailed"));
    }
  }

  async function handleQuickSell(sp: SquadPlayer) {
    const payout = Math.round(sp.currentValue * 0.5);
    if (!confirm(t("league.team.quickSellConfirm", {
      name: sp.playerName,
      amount: moneyFull(payout),
    }))) return;
    try {
      const result = await quickSell(leagueId, sp.squadPlayerId);
      toast.success(t("league.team.quickSellDone", {
        name: sp.playerName,
        amount: moneyFull(result.amountCredited),
      }));
      invalidateSquad();
    } catch {
      toast.error(t("league.team.quickSellFailed"));
    }
  }

  return (
    <div className="p-4 max-w-lg mx-auto">
      <div className="mb-4">
        <div className="bg-white border border-zinc-200 shadow-sm rounded-xl px-3 py-2.5 flex items-center gap-2 max-w-xs">
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
      </div>

      <h2 className="text-xl font-bold text-zinc-900 mb-4">{t("league.team.title")}</h2>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500" />
        </div>
      ) : squad && squad.length > 0 ? (
        <div className="space-y-3">
          {squad.map((sp) => {
            const img = resolvePlayerImage(sp.imageRef, sp.playerName);
            return (
              <div
                key={sp.squadPlayerId}
                className="bg-white border border-zinc-200 shadow-sm rounded-xl p-4"
              >
                <div className="flex items-center gap-3">
                  <div
                    className="w-11 h-11 rounded-full flex items-center justify-center text-sm font-bold text-white flex-shrink-0 overflow-hidden"
                    style={{ background: img.showImage ? undefined : getAvatarColor(sp.playerName) }}
                  >
                    {img.showImage ? (
                      <img
                        src={img.src!}
                        alt={sp.playerName}
                        className="w-11 h-11 rounded-full object-cover"
                      />
                    ) : (
                      img.initials
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-0.5">
                      <span
                        className={`text-[10px] font-semibold uppercase tracking-wide px-1.5 py-0.5 rounded ${positionColors[sp.position]}`}
                      >
                        {sp.position}
                      </span>
                      <span className="font-semibold text-zinc-900 truncate">
                        {sp.playerName}
                      </span>
                    </div>
                    <span className="text-xs text-zinc-500">{sp.nationalTeam}</span>
                  </div>
                  <div className="text-right flex-shrink-0 flex flex-col items-end gap-1">
                    <div className="text-sm font-bold text-emerald-600 tabular-nums">
                      {moneyCompact(sp.currentValue)}
                    </div>
                    <button
                      type="button"
                      onClick={() => setClausePlayer(sp)}
                      className="text-xs text-zinc-500 hover:text-zinc-900 inline-flex items-center gap-1 transition-colors"
                    >
                      {t("league.team.releaseClause")}:{" "}
                      <span className="tabular-nums">{moneyCompact(sp.releaseClause)}</span>
                      <Pencil className="h-3 w-3" />
                    </button>
                  </div>
                </div>

                <div className="mt-3 pt-3 border-t border-zinc-100 flex gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 border-zinc-300 text-zinc-700 hover:text-zinc-900 gap-1 text-xs h-8"
                    onClick={() => handleListForSale(sp)}
                  >
                    <Tag className="h-3 w-3" /> {t("league.team.listForSale")}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1 border-rose-200 text-rose-700 hover:text-rose-900 gap-1 text-xs h-8"
                    onClick={() => handleQuickSell(sp)}
                    title={t("league.team.quickSellHint")}
                  >
                    <Zap className="h-3 w-3" /> {t("league.team.quickSell")}
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="text-center py-12 text-zinc-500">
          <p>{t("league.team.empty")}</p>
          <p className="text-xs mt-1">{t("league.team.goToMarket")}</p>
        </div>
      )}

      {clausePlayer && (
        <UpdateClauseModal
          leagueId={leagueId}
          player={clausePlayer}
          onClose={() => setClausePlayer(null)}
          onSaved={() => {
            queryClient.invalidateQueries({ queryKey: ["squad", leagueId] });
            queryClient.invalidateQueries({ queryKey: ["activity", leagueId] });
          }}
        />
      )}

      {pendingOffer && (
        <OfferModal
          playerName={pendingOffer.playerName}
          offer={pendingOffer.offer}
          onAccept={handleAcceptOffer}
          onClose={() => setPendingOffer(null)}
        />
      )}
    </div>
  );
}

function UpdateClauseModal({
  leagueId,
  player,
  onClose,
  onSaved,
}: {
  leagueId: string;
  player: SquadPlayer;
  onClose: () => void;
  onSaved: () => void;
}) {
  const t = useT();
  const [value, setValue] = useState<string>(
    Math.ceil(player.releaseClause + 1).toString(),
  );
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  async function handleSave() {
    const parsed = Number(value);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      toast.error(t("common.error"));
      return;
    }
    if (parsed <= player.releaseClause) {
      toast.error(t("common.error"));
      return;
    }
    setSaving(true);
    try {
      await updateReleaseClause(leagueId, player.playerId, parsed);
      toast.success(t("common.save"));
      onSaved();
      onClose();
    } catch (e: unknown) {
      const msg =
        (e as { response?: { data?: { error?: string } } })?.response?.data?.error ||
        t("common.error");
      toast.error(msg);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="w-full sm:max-w-sm bg-white rounded-t-2xl sm:rounded-2xl border border-zinc-200 shadow-xl p-5"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between mb-4">
          <div>
            <h3 className="font-semibold text-zinc-900">{t("league.team.releaseClause")}</h3>
            <p className="text-xs text-zinc-500 mt-0.5">{player.playerName}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-zinc-500 hover:text-zinc-900 transition-colors"
            aria-label={t("common.close")}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-3">
          <div className="text-xs text-zinc-500 flex justify-between">
            <span>{t("league.team.releaseClause")}</span>
            <span className="tabular-nums text-zinc-700">
              {moneyFull(player.releaseClause)}
            </span>
          </div>

          <div className="space-y-1">
            <Label htmlFor="clause">{t("common.edit")}</Label>
            <Input
              id="clause"
              type="number"
              inputMode="numeric"
              min={Math.ceil(player.releaseClause + 1)}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              className="bg-white border-zinc-300 tabular-nums"
              autoFocus
            />
          </div>

          <div className="flex gap-2 pt-2">
            <Button
              variant="outline"
              onClick={onClose}
              className="flex-1 border-zinc-300 text-zinc-700"
              disabled={saving}
            >
              {t("common.cancel")}
            </Button>
            <Button
              onClick={handleSave}
              disabled={saving}
              className="flex-1 bg-blue-600 hover:bg-blue-700"
            >
              {saving ? t("common.saving") : t("common.save")}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function OfferModal({
  playerName,
  offer,
  onAccept,
  onClose,
}: {
  playerName: string;
  offer: MachineOffer;
  onAccept: () => void | Promise<void>;
  onClose: () => void;
}) {
  const t = useT();
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const expires = new Date(offer.expiresAt);

  return (
    <div
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/40 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="w-full sm:max-w-sm bg-white rounded-t-2xl sm:rounded-2xl border border-zinc-200 shadow-xl p-5"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between mb-4">
          <div>
            <h3 className="font-semibold text-zinc-900">{t("league.team.machineOffer")}</h3>
            <p className="text-xs text-zinc-500 mt-0.5">{playerName}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-zinc-500 hover:text-zinc-900 transition-colors"
            aria-label={t("common.close")}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="space-y-3">
          <div className="bg-zinc-50 border border-zinc-200 rounded-xl px-3 py-3 text-center">
            <div className="text-xs uppercase tracking-wide text-zinc-500 mb-1">
              {t("league.team.machineOffer")}
            </div>
            <div className="text-2xl font-bold text-emerald-600 tabular-nums">
              {moneyFull(offer.amount)}
            </div>
            <div className="text-[11px] text-zinc-500 mt-1">
              {t("league.team.offerExpires", { date: expires.toLocaleString() })}
            </div>
          </div>

          <div className="flex gap-2 pt-2">
            <Button
              variant="outline"
              onClick={onClose}
              className="flex-1 border-zinc-300 text-zinc-700"
            >
              {t("common.cancel")}
            </Button>
            <Button
              onClick={onAccept}
              className="flex-1 bg-emerald-600 hover:bg-emerald-700"
            >
              {t("league.team.acceptOffer")}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
