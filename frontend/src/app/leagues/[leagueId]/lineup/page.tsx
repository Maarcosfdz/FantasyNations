"use client";

import { use, useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getSquad, type SquadPlayer } from "@/shared/api/squadApi";
import { getAvatarColor } from "@/shared/assets/fallbackAvatar";
import { resolvePlayerImage } from "@/shared/assets/playerImageResolver";
import apiClient from "@/shared/api/apiClient";
import {
  getMatchdays,
  getMatchdayScore,
  type MatchdayListItem,
} from "@/shared/api/matchdayApi";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import {
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  Save as SaveIcon,
  Search,
  X,
  Trash2,
} from "lucide-react";
import { useT } from "@/shared/i18n/I18nProvider";

interface Props {
  params: Promise<{ leagueId: string }>;
}

type Position = "GK" | "DEF" | "MID" | "FWD";
type FormationCode =
  | "1-3-4-3" | "1-3-5-2" | "1-3-6-1"
  | "1-4-3-3" | "1-4-4-2" | "1-4-5-1"
  | "1-5-2-3" | "1-5-3-2" | "1-5-4-1";

const FORMATIONS: FormationCode[] = [
  "1-3-4-3", "1-3-5-2", "1-3-6-1",
  "1-4-3-3", "1-4-4-2", "1-4-5-1",
  "1-5-2-3", "1-5-3-2", "1-5-4-1",
];
const DEFAULT_FORMATION: FormationCode = "1-4-4-2";

interface Slot {
  id: string;
  position: Position;
  row: number;
  /** Horizontal position 0..100 inside the pitch, used to lay cards out. */
  xPct: number;
}

function slotsFor(code: FormationCode): Slot[] {
  const [, defStr, midStr, fwdStr] = code.split("-");
  const def = Number(defStr), mid = Number(midStr), fwd = Number(fwdStr);
  const slots: Slot[] = [{ id: "GK-1", position: "GK", row: 1, xPct: 50 }];
  const spread = (n: number) =>
    n === 1 ? [50] : Array.from({ length: n }, (_, i) => 10 + (80 / (n - 1)) * i);
  spread(def).forEach((x, i) => slots.push({ id: `DEF-${i + 1}`, position: "DEF", row: 2, xPct: x }));
  spread(mid).forEach((x, i) => slots.push({ id: `MID-${i + 1}`, position: "MID", row: 3, xPct: x }));
  spread(fwd).forEach((x, i) => slots.push({ id: `FWD-${i + 1}`, position: "FWD", row: 4, xPct: x }));
  return slots;
}

/** Vertical position (0..100) on the pitch for a given row. GK at the back. */
function rowYPct(row: number): number {
  return [0, 88, 68, 48, 26][row]!;
}

const stageLabel: Record<MatchdayListItem["phase"], string> = {
  GROUP: "Group",
  R16: "Round of 16",
  QF: "Quarter-finals",
  SF: "Semi-finals",
  FINAL: "Final",
};

// ────────────────────────────────────────────────────────────────────────────
// Page
// ────────────────────────────────────────────────────────────────────────────
export default function LineupPage({ params }: Props) {
  const { leagueId } = use(params);
  const queryClient = useQueryClient();
  const t = useT();

  const { data: squad } = useQuery({
    queryKey: ["squad", leagueId],
    queryFn: () => getSquad(leagueId),
  });

  const { data: matchdays } = useQuery({
    queryKey: ["matchdays", leagueId],
    queryFn: () => getMatchdays(leagueId),
  });

  const { data: savedLineup } = useQuery<Record<string, string>>({
    queryKey: ["lineup", leagueId],
    queryFn: async () => {
      const { data } = await apiClient.get<
        { playerId: string; positionSlot: string }[]
      >(`/api/leagues/${leagueId}/lineup`);
      const map: Record<string, string> = {};
      data.forEach((p) => { map[p.positionSlot] = p.playerId; });
      return map;
    },
  });

  // Default to the first matchday with status != FINISHED, else the last.
  const defaultMatchdayId = useMemo(() => {
    if (!matchdays || matchdays.length === 0) return null;
    const currentOrUpcoming = matchdays.find((m) => m.status !== "FINISHED");
    return (currentOrUpcoming ?? matchdays[matchdays.length - 1]).id;
  }, [matchdays]);
  const [activeMatchdayId, setActiveMatchdayId] = useState<string | null>(null);
  useEffect(() => {
    if (!activeMatchdayId && defaultMatchdayId) setActiveMatchdayId(defaultMatchdayId);
  }, [defaultMatchdayId, activeMatchdayId]);
  const activeMatchday = matchdays?.find((m) => m.id === activeMatchdayId) ?? null;

  // Score mode is on when viewing a FINISHED matchday.
  const isScoreMode = activeMatchday?.status === "FINISHED";

  const { data: matchdayScore } = useQuery({
    queryKey: ["matchdayScore", leagueId, activeMatchdayId],
    queryFn: () => getMatchdayScore(leagueId, activeMatchdayId!),
    enabled: !!activeMatchdayId && isScoreMode,
  });

  // --- formation + lineup state (only for the editable mode) ---
  const [lineup, setLineup] = useState<Record<string, string>>({});
  const [formation, setFormation] = useState<FormationCode>(DEFAULT_FORMATION);

  // Sync from server-saved lineup. Infer formation from the saved slot list.
  useEffect(() => {
    if (!savedLineup) return;
    setLineup(savedLineup);
    const counts = { GK: 0, DEF: 0, MID: 0, FWD: 0 };
    for (const slot of Object.keys(savedLineup)) {
      const pos = slot.split("-")[0] as keyof typeof counts;
      if (counts[pos] != null) counts[pos]++;
    }
    const code = `${counts.GK}-${counts.DEF}-${counts.MID}-${counts.FWD}` as FormationCode;
    if (FORMATIONS.includes(code)) setFormation(code);
  }, [savedLineup]);

  function changeFormation(next: FormationCode) {
    const nextSlots = slotsFor(next);
    setLineup((prev) => {
      const pool: Record<string, string[]> = { GK: [], DEF: [], MID: [], FWD: [] };
      for (const [slot, pid] of Object.entries(prev)) {
        const pos = slot.split("-")[0];
        if (pool[pos]) pool[pos].push(pid);
      }
      const remapped: Record<string, string> = {};
      for (const s of nextSlots) {
        const q = pool[s.position];
        if (q && q.length) remapped[s.id] = q.shift()!;
      }
      return remapped;
    });
    setFormation(next);
  }

  // --- save flow ---
  const [saveState, setSaveState] = useState<"idle" | "saving" | "saved">("idle");
  async function handleSave() {
    setSaveState("saving");
    try {
      const payload: Record<string, string> = {};
      for (const [slot, playerId] of Object.entries(lineup)) {
        payload[playerId] = slot;
      }
      await apiClient.put(`/api/leagues/${leagueId}/lineup`, payload);
      setSaveState("saved");
      toast.success(t("league.lineup.saved"));
      queryClient.invalidateQueries({ queryKey: ["lineup", leagueId] });
      setTimeout(() => setSaveState("idle"), 1800);
    } catch (e: unknown) {
      setSaveState("idle");
      const msg = (e as { response?: { data?: { error?: string } } })
        ?.response?.data?.error ?? t("league.lineup.saveFailed");
      toast.error(msg);
    }
  }

  // --- panels / sheets ---
  const [formationSheetOpen, setFormationSheetOpen] = useState(false);
  const [matchdaySheetOpen, setMatchdaySheetOpen] = useState(false);
  const [pickerSlot, setPickerSlot] = useState<Slot | null>(null);

  function assignToSlot(slot: Slot, playerId: string) {
    setLineup((prev) => {
      const next: Record<string, string> = {};
      for (const [s, pid] of Object.entries(prev)) if (pid !== playerId) next[s] = pid;
      next[slot.id] = playerId;
      return next;
    });
  }
  function clearSlot(slot: Slot) {
    setLineup((prev) => {
      const next = { ...prev }; delete next[slot.id]; return next;
    });
  }

  // --- matchday navigation arrows ---
  function shiftMatchday(delta: number) {
    if (!matchdays || !activeMatchdayId) return;
    const i = matchdays.findIndex((m) => m.id === activeMatchdayId);
    const j = i + delta;
    if (j >= 0 && j < matchdays.length) setActiveMatchdayId(matchdays[j].id);
  }

  // --- score-mode lookup ---
  const scoreByPlayer = useMemo(() => {
    if (!matchdayScore) return new Map<string, { points: number; positionSlot: string }>();
    return new Map(
      matchdayScore.players.map((p) => [p.playerId, { points: p.points, positionSlot: p.positionSlot }]),
    );
  }, [matchdayScore]);

  // In score mode the lineup we render is the frozen snapshot from the API.
  const renderedSlots = useMemo(() => {
    if (isScoreMode && matchdayScore) {
      const counts = { GK: 0, DEF: 0, MID: 0, FWD: 0 };
      matchdayScore.players.forEach((p) => {
        const pos = p.positionSlot.split("-")[0] as keyof typeof counts;
        if (counts[pos] != null) counts[pos]++;
      });
      const code = `${counts.GK}-${counts.DEF}-${counts.MID}-${counts.FWD}` as FormationCode;
      return FORMATIONS.includes(code) ? slotsFor(code) : slotsFor(DEFAULT_FORMATION);
    }
    return slotsFor(formation);
  }, [isScoreMode, matchdayScore, formation]);

  const renderedAssignments = useMemo<Record<string, string>>(() => {
    if (isScoreMode && matchdayScore) {
      const m: Record<string, string> = {};
      matchdayScore.players.forEach((p) => { m[p.positionSlot] = p.playerId; });
      return m;
    }
    return lineup;
  }, [isScoreMode, matchdayScore, lineup]);

  const filled = Object.keys(renderedAssignments).length;
  const playersById = useMemo(() => {
    const m = new Map<string, SquadPlayer>();
    (squad ?? []).forEach((p) => m.set(p.playerId, p));
    // Seed score-mode players from the API so they render even if they left
    // the squad after the snapshot.
    matchdayScore?.players.forEach((p) => {
      if (!m.has(p.playerId)) {
        m.set(p.playerId, {
          squadPlayerId: p.playerId,
          playerId: p.playerId,
          playerName: p.playerName,
          nationalTeam: p.nationalTeam,
          position: p.position as Position,
          imageRef: p.imageRef,
          currentValue: 0,
          releaseClause: 0,
          protectedUntil: null,
          acquiredAt: "",
        });
      }
    });
    return m;
  }, [squad, matchdayScore]);

  return (
    <div className="p-3 sm:p-4 max-w-lg mx-auto pb-24">
      <Header
        matchday={activeMatchday}
        onPrev={() => shiftMatchday(-1)}
        onNext={() => shiftMatchday(+1)}
        onSelectMatchday={() => setMatchdaySheetOpen(true)}
        saveState={saveState}
        onSave={handleSave}
        showSave={!isScoreMode}
        t={t}
      />

      {isScoreMode && activeMatchday && (
        <ScoreBar
          matchday={activeMatchday}
          total={matchdayScore?.totalPoints ?? activeMatchday.myTotalPoints ?? 0}
          t={t}
        />
      )}

      <Pitch>
        {!isScoreMode && (
          <button
            onClick={() => setFormationSheetOpen(true)}
            className="absolute top-2 left-2 z-20 inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-white/90 backdrop-blur text-xs font-semibold text-zinc-900 shadow-sm hover:bg-white"
          >
            <FormationGlyph code={formation} />
            <span className="text-zinc-500 font-medium">{t("league.lineup.formation")}</span>
            <span className="tabular-nums">{formation}</span>
            <ChevronDown className="h-3 w-3 text-zinc-500" />
          </button>
        )}

        {activeMatchday && (
          <div className="absolute top-2 right-2 z-20 px-2.5 py-1 rounded-full bg-black/55 backdrop-blur text-white text-[10px] font-bold uppercase tracking-wider flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-pink-400" />
            {stageLabel[activeMatchday.phase]}
          </div>
        )}

        <div className="absolute inset-0 z-10">
          {renderedSlots.map((slot) => {
            const playerId = renderedAssignments[slot.id];
            const player = playerId ? playersById.get(playerId) : undefined;
            const score = isScoreMode && playerId ? scoreByPlayer.get(playerId)?.points ?? 0 : null;
            return (
              <div
                key={slot.id}
                className="absolute -translate-x-1/2 -translate-y-1/2"
                style={{ left: `${slot.xPct}%`, top: `${rowYPct(slot.row)}%` }}
              >
                <PlayerChromo
                  slot={slot}
                  player={player}
                  score={score}
                  readonly={isScoreMode}
                  onClick={() => !isScoreMode && setPickerSlot(slot)}
                />
              </div>
            );
          })}
        </div>
      </Pitch>

      <div className="mt-3 flex items-center justify-between text-xs text-zinc-500">
        <span>{t("league.lineup.title")} · {filled}/11</span>
        {!isScoreMode && (
          <span>{t("league.lineup.formation")}: {formation}</span>
        )}
      </div>

      {!squad?.length && (
        <p className="mt-4 text-sm text-zinc-500 text-center">{t("league.team.goToMarket")}</p>
      )}

      {formationSheetOpen && (
        <FormationSheet
          current={formation}
          onPick={(f) => { changeFormation(f); setFormationSheetOpen(false); }}
          onClose={() => setFormationSheetOpen(false)}
          t={t}
        />
      )}
      {matchdaySheetOpen && matchdays && (
        <MatchdaySheet
          matchdays={matchdays}
          current={activeMatchdayId}
          onPick={(id) => { setActiveMatchdayId(id); setMatchdaySheetOpen(false); }}
          onClose={() => setMatchdaySheetOpen(false)}
          t={t}
        />
      )}
      {pickerSlot && (
        <PlayerPickerSheet
          slot={pickerSlot}
          squad={squad ?? []}
          currentPlayerId={renderedAssignments[pickerSlot.id] ?? null}
          alreadyInLineup={new Set(Object.values(renderedAssignments))}
          onPick={(pid) => { assignToSlot(pickerSlot, pid); setPickerSlot(null); }}
          onClear={() => { clearSlot(pickerSlot); setPickerSlot(null); }}
          onClose={() => setPickerSlot(null)}
          t={t}
        />
      )}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────
// Header — title row + matchday selector + save button
// ────────────────────────────────────────────────────────────────────────────
function Header({
  matchday, onPrev, onNext, onSelectMatchday,
  saveState, onSave, showSave, t,
}: {
  matchday: MatchdayListItem | null;
  onPrev: () => void;
  onNext: () => void;
  onSelectMatchday: () => void;
  saveState: "idle" | "saving" | "saved";
  onSave: () => void;
  showSave: boolean;
  t: ReturnType<typeof useT>;
}) {
  const saveLabel =
    saveState === "saving" ? t("common.saving")
    : saveState === "saved" ? t("league.lineup.saved")
    : t("league.lineup.save");
  const saveBg =
    saveState === "saved" ? "bg-emerald-600 hover:bg-emerald-700"
    : "bg-blue-600 hover:bg-blue-700";
  return (
    <div className="mb-3">
      <div className="flex items-end justify-between mb-3">
        <div>
          <div className="text-[10px] font-bold tracking-widest text-blue-600 uppercase">
            FantasyNations
          </div>
          <h2 className="text-2xl font-extrabold text-zinc-900 tracking-tight leading-none">
            {t("league.lineup.title")}
          </h2>
        </div>
        {showSave && (
          <Button
            onClick={onSave}
            disabled={saveState !== "idle"}
            size="sm"
            className={`${saveBg} text-white font-bold gap-1.5`}
          >
            {saveState === "saving" ? (
              <span className="inline-block h-3 w-3 rounded-full border-2 border-white border-t-transparent animate-spin" />
            ) : (
              <SaveIcon className="h-3.5 w-3.5" />
            )}
            {saveLabel}
          </Button>
        )}
      </div>

      <div className="flex items-stretch gap-2 bg-white border border-zinc-200 rounded-2xl p-2 shadow-sm">
        <button
          onClick={onPrev}
          className="w-8 h-8 rounded-lg bg-zinc-100 hover:bg-zinc-200 flex items-center justify-center"
          aria-label="Previous matchday"
        >
          <ChevronLeft className="h-4 w-4 text-zinc-700" />
        </button>
        <button
          onClick={onSelectMatchday}
          className="flex-1 flex items-center justify-between px-2 text-left"
        >
          <div>
            <div className="text-[10px] font-bold uppercase tracking-wider text-zinc-500">
              {matchday ? `${stageLabel[matchday.phase]} · ${t("league.lineup.matchday")} ${matchday.number}` : "—"}
            </div>
            <div className="text-sm font-bold text-zinc-900 mt-0.5">
              {matchday?.status === "FINISHED" ? (
                <>
                  <span className="text-zinc-500 mr-1">{t("league.ranking.points")}:</span>
                  <span className="tabular-nums">{matchday.myTotalPoints ?? 0}</span>
                </>
              ) : matchday?.lockAt ? (
                <>
                  <span className="text-zinc-500 mr-1">{t("league.lineup.locksAt")}</span>
                  <span className="text-blue-600 tabular-nums">
                    {new Date(matchday.lockAt).toLocaleString(undefined, {
                      month: "short", day: "numeric", hour: "2-digit", minute: "2-digit",
                    })}
                  </span>
                </>
              ) : (
                <span className="text-zinc-500">{t("league.lineup.title")}</span>
              )}
            </div>
          </div>
          <ChevronDown className="h-4 w-4 text-zinc-400" />
        </button>
        <button
          onClick={onNext}
          className="w-8 h-8 rounded-lg bg-zinc-100 hover:bg-zinc-200 flex items-center justify-center"
          aria-label="Next matchday"
        >
          <ChevronRight className="h-4 w-4 text-zinc-700" />
        </button>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────
// Score bar — total points + matchday label (frozen mode)
// ────────────────────────────────────────────────────────────────────────────
function ScoreBar({
  matchday, total, t,
}: { matchday: MatchdayListItem; total: number; t: ReturnType<typeof useT> }) {
  return (
    <div className="flex items-center gap-2.5 px-3 py-2 rounded-xl mb-3 bg-zinc-900 text-white shadow-md">
      <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-amber-300 to-amber-500 text-amber-900 font-black text-xs flex items-center justify-center shadow-inner">
        J{matchday.number}
      </div>
      <div className="flex-1 min-w-0">
        <div className="text-xs font-bold truncate">
          {t("league.lineup.title")} · {t("league.lineup.matchday")} {matchday.number}
        </div>
        <div className="text-[10px] font-medium opacity-70 mt-0.5">
          {stageLabel[matchday.phase]}
        </div>
      </div>
      <div className="flex items-baseline gap-1 tabular-nums">
        <span className="text-2xl font-black text-amber-300">{total}</span>
        <span className="text-[10px] font-bold opacity-70 uppercase tracking-wider">
          {t("league.ranking.points")}
        </span>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────
// Pitch — 3D-ish football field via SVG + gradients
// ────────────────────────────────────────────────────────────────────────────
function Pitch({ children }: { children: React.ReactNode }) {
  return (
    <div
      className="relative w-full rounded-2xl overflow-hidden shadow-lg"
      style={{ aspectRatio: "3 / 4" }}
    >
      <svg
        viewBox="0 0 300 400"
        preserveAspectRatio="xMidYMid slice"
        className="absolute inset-0 w-full h-full"
      >
        <defs>
          <linearGradient id="pitchGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#1e6b3a" />
            <stop offset="50%" stopColor="#2a8a48" />
            <stop offset="100%" stopColor="#1f6b39" />
          </linearGradient>
          <radialGradient id="pitchLight" cx="50%" cy="20%" r="70%">
            <stop offset="0%" stopColor="rgba(255,255,255,0.18)" />
            <stop offset="60%" stopColor="rgba(255,255,255,0)" />
          </radialGradient>
          <radialGradient id="pitchVignette" cx="50%" cy="50%" r="70%">
            <stop offset="60%" stopColor="rgba(0,0,0,0)" />
            <stop offset="100%" stopColor="rgba(0,0,0,0.45)" />
          </radialGradient>
        </defs>
        <rect width="300" height="400" fill="url(#pitchGrad)" />
        {[0, 1, 2, 3, 4, 5, 6, 7].map((i) => (
          <rect
            key={i}
            x="0"
            y={i * 50}
            width="300"
            height="50"
            fill={i % 2 === 0 ? "rgba(255,255,255,0.04)" : "rgba(0,0,0,0.05)"}
          />
        ))}
        <rect x="10" y="10" width="280" height="380" fill="none" stroke="rgba(255,255,255,0.55)" strokeWidth="1.4" />
        <line x1="10" y1="200" x2="290" y2="200" stroke="rgba(255,255,255,0.55)" strokeWidth="1.2" />
        <circle cx="150" cy="200" r="32" fill="none" stroke="rgba(255,255,255,0.55)" strokeWidth="1.2" />
        <circle cx="150" cy="200" r="2" fill="rgba(255,255,255,0.6)" />
        <rect x="70" y="10" width="160" height="55" fill="none" stroke="rgba(255,255,255,0.55)" strokeWidth="1.2" />
        <rect x="100" y="10" width="100" height="22" fill="none" stroke="rgba(255,255,255,0.55)" strokeWidth="1.2" />
        <rect x="70" y="335" width="160" height="55" fill="none" stroke="rgba(255,255,255,0.55)" strokeWidth="1.2" />
        <rect x="100" y="368" width="100" height="22" fill="none" stroke="rgba(255,255,255,0.55)" strokeWidth="1.2" />
        <rect width="300" height="400" fill="url(#pitchLight)" />
        <rect width="300" height="400" fill="url(#pitchVignette)" />
      </svg>
      {children}
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────
// Player chromo — trading-card style
// ────────────────────────────────────────────────────────────────────────────
const POS_COLORS: Record<Position, { ring: string; chip: string }> = {
  GK:  { ring: "ring-amber-300/60",   chip: "bg-amber-100 text-amber-800"   },
  DEF: { ring: "ring-sky-300/60",     chip: "bg-sky-100 text-sky-800"       },
  MID: { ring: "ring-emerald-300/60", chip: "bg-emerald-100 text-emerald-800" },
  FWD: { ring: "ring-rose-300/60",    chip: "bg-rose-100 text-rose-800"     },
};

function PlayerChromo({
  slot, player, score, readonly, onClick,
}: {
  slot: Slot;
  player?: SquadPlayer;
  score: number | null;
  readonly: boolean;
  onClick: () => void;
}) {
  const pc = POS_COLORS[slot.position];
  if (!player) {
    return (
      <button
        onClick={onClick}
        disabled={readonly}
        className="w-16 h-20 rounded-xl bg-white/85 backdrop-blur-sm ring-1 ring-white/70 flex flex-col items-center justify-center text-zinc-500 hover:bg-white transition disabled:cursor-default"
      >
        <div className={`text-[9px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded ${pc.chip} mb-1`}>
          {slot.position}
        </div>
        <div className="text-[10px] font-medium opacity-70">+</div>
      </button>
    );
  }
  const img = resolvePlayerImage(player.imageRef, player.playerName);
  return (
    <button
      onClick={onClick}
      disabled={readonly}
      className={`w-16 rounded-xl bg-white shadow-md hover:shadow-lg transition ring-1 ${pc.ring} flex flex-col items-center pt-1 pb-0.5 disabled:cursor-default relative`}
    >
      {score != null && (
        <span
          className={`absolute -top-2 -right-2 z-10 min-w-[22px] h-[22px] px-1.5 rounded-full text-[11px] font-black tabular-nums shadow flex items-center justify-center ${
            score > 0 ? "bg-amber-400 text-amber-900"
              : score < 0 ? "bg-rose-500 text-white"
              : "bg-zinc-300 text-zinc-800"
          }`}
        >
          {score}
        </span>
      )}
      <div className={`text-[8px] font-bold uppercase tracking-wider px-1 py-0.5 rounded ${pc.chip} mb-0.5`}>
        {slot.position}
      </div>
      <div
        className="w-12 h-12 rounded-full overflow-hidden flex items-center justify-center text-white font-bold text-sm shadow-inner"
        style={{ background: img.showImage ? undefined : getAvatarColor(player.playerName) }}
      >
        {img.showImage ? (
          <img src={img.src!} alt={player.playerName} className="w-12 h-12 object-cover" />
        ) : (
          img.initials
        )}
      </div>
      <div className="mt-0.5 w-full px-1 text-center">
        <div className="text-[9px] font-bold text-zinc-900 truncate leading-tight">
          {lastName(player.playerName)}
        </div>
        <div className="text-[7px] text-zinc-500 truncate uppercase tracking-wider">
          {player.nationalTeam}
        </div>
      </div>
    </button>
  );
}

function lastName(full: string): string {
  const parts = full.trim().split(/\s+/);
  return parts.length === 1 ? parts[0] : parts.slice(1).join(" ");
}

// ────────────────────────────────────────────────────────────────────────────
// Formation glyph + diagram
// ────────────────────────────────────────────────────────────────────────────
function FormationGlyph({ code }: { code: FormationCode }) {
  const [, defStr, midStr, fwdStr] = code.split("-");
  const rows = [Number(defStr), Number(midStr), Number(fwdStr)];
  return (
    <span className="inline-block w-5 h-4 rounded bg-blue-100 p-0.5">
      <svg viewBox="0 0 22 18" className="w-full h-full">
        <circle cx="11" cy="2.5" r="1.1" className="fill-blue-600" />
        {rows.map((count, rowIdx) => {
          const y = 6 + rowIdx * 3.5;
          return Array.from({ length: count }, (_, i) => {
            const x = count === 1 ? 11 : 2 + (18 / (count - 1)) * i;
            return <circle key={`${rowIdx}-${i}`} cx={x} cy={y} r="1.1" className="fill-blue-600" />;
          });
        })}
      </svg>
    </span>
  );
}

function FormationDiagram({ code, active }: { code: FormationCode; active: boolean }) {
  const [, defStr, midStr, fwdStr] = code.split("-");
  const rows = [Number(defStr), Number(midStr), Number(fwdStr)];
  const fill = active ? "fill-blue-600" : "fill-zinc-400";
  return (
    <svg viewBox="0 0 72 50" className="w-16 h-12">
      <rect x="0.5" y="0.5" width="71" height="49" rx="4" className={active ? "fill-blue-50 stroke-blue-200" : "fill-zinc-50 stroke-zinc-200"} strokeWidth="1" />
      <line x1="0" y1="25" x2="72" y2="25" className={active ? "stroke-blue-200" : "stroke-zinc-300"} strokeWidth="0.5" />
      <circle cx="36" cy="25" r="5" fill="none" className={active ? "stroke-blue-200" : "stroke-zinc-300"} strokeWidth="0.5" />
      <circle cx="36" cy="44" r="2.2" className={fill} />
      {rows.map((count, rowIdx) => {
        const y = 34 - rowIdx * 10;
        return Array.from({ length: count }, (_, i) => {
          const x = count === 1 ? 36 : 8 + ((72 - 16) / (count - 1)) * i;
          return <circle key={`${rowIdx}-${i}`} cx={x} cy={y} r="2.2" className={fill} />;
        });
      })}
    </svg>
  );
}

// ────────────────────────────────────────────────────────────────────────────
// Bottom sheet shell
// ────────────────────────────────────────────────────────────────────────────
function BottomSheet({
  title, onClose, children,
}: { title: string; onClose: () => void; children: React.ReactNode }) {
  useEffect(() => {
    function onKey(e: KeyboardEvent) { if (e.key === "Escape") onClose(); }
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);
  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg bg-white rounded-t-2xl shadow-2xl max-h-[88vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mx-auto w-9 h-1 rounded-full bg-zinc-300 mt-2 mb-2" />
        <div className="flex items-center justify-between px-4 pb-2">
          <h3 className="font-bold text-zinc-900">{title}</h3>
          <button onClick={onClose} className="text-zinc-500 hover:text-zinc-900">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="overflow-y-auto px-3 pb-5">{children}</div>
      </div>
    </div>
  );
}

// ────────────────────────────────────────────────────────────────────────────
// Formation sheet
// ────────────────────────────────────────────────────────────────────────────
function FormationSheet({
  current, onPick, onClose, t,
}: {
  current: FormationCode;
  onPick: (f: FormationCode) => void;
  onClose: () => void;
  t: ReturnType<typeof useT>;
}) {
  return (
    <BottomSheet title={t("league.lineup.formation")} onClose={onClose}>
      <div className="grid grid-cols-3 gap-2 pt-2">
        {FORMATIONS.map((f) => {
          const active = f === current;
          return (
            <button
              key={f}
              onClick={() => onPick(f)}
              className={`flex flex-col items-center gap-1.5 px-2 py-2.5 rounded-xl border ${
                active ? "border-blue-600 bg-blue-50" : "border-zinc-200 bg-white hover:bg-zinc-50"
              }`}
            >
              <FormationDiagram code={f} active={active} />
              <span className={`text-xs font-bold tracking-wide ${active ? "text-blue-700" : "text-zinc-700"}`}>
                {f}
              </span>
            </button>
          );
        })}
      </div>
    </BottomSheet>
  );
}

// ────────────────────────────────────────────────────────────────────────────
// Matchday sheet
// ────────────────────────────────────────────────────────────────────────────
function MatchdaySheet({
  matchdays, current, onPick, onClose, t,
}: {
  matchdays: MatchdayListItem[];
  current: string | null;
  onPick: (id: string) => void;
  onClose: () => void;
  t: ReturnType<typeof useT>;
}) {
  return (
    <BottomSheet title={t("league.lineup.matchday")} onClose={onClose}>
      <div className="flex flex-col gap-1.5 pt-2">
        {matchdays.map((m) => {
          const active = m.id === current;
          const finished = m.status === "FINISHED";
          return (
            <button
              key={m.id}
              onClick={() => onPick(m.id)}
              className={`flex items-center justify-between p-3 rounded-xl text-left transition ${
                active ? "bg-blue-50 border border-blue-600"
                       : "bg-zinc-50 hover:bg-zinc-100 border border-transparent"
              }`}
            >
              <div className="flex items-center gap-3">
                <div
                  className={`w-9 h-9 rounded-lg font-extrabold flex items-center justify-center text-xs ${
                    active ? "bg-blue-600 text-white"
                           : finished ? "bg-zinc-200 text-zinc-700"
                           : "bg-zinc-900 text-white"
                  }`}
                >
                  J{m.number}
                </div>
                <div>
                  <div className="text-sm font-bold text-zinc-900">
                    {stageLabel[m.phase]} · {t("league.lineup.matchday")} {m.number}
                  </div>
                  <div className="text-[11px] text-zinc-500 mt-0.5">
                    {finished ? t("league.lineup.frozenStatus")
                      : m.status === "LOCKED" ? t("league.lineup.lockedStatus")
                      : m.lockAt ? `${t("league.lineup.locksAt")} ${new Date(m.lockAt).toLocaleString(undefined, {
                          month: "short", day: "numeric", hour: "2-digit", minute: "2-digit",
                        })}`
                      : ""}
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className="text-sm font-extrabold tabular-nums text-zinc-900">
                  {m.myTotalPoints == null ? "—" : m.myTotalPoints}
                </div>
                <div className="text-[9px] text-zinc-500 uppercase tracking-wider">
                  {t("league.ranking.points")}
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </BottomSheet>
  );
}

// ────────────────────────────────────────────────────────────────────────────
// Player picker sheet — filtered by position + search
// ────────────────────────────────────────────────────────────────────────────
function PlayerPickerSheet({
  slot, squad, currentPlayerId, alreadyInLineup,
  onPick, onClear, onClose, t,
}: {
  slot: Slot;
  squad: SquadPlayer[];
  currentPlayerId: string | null;
  alreadyInLineup: Set<string>;
  onPick: (playerId: string) => void;
  onClear: () => void;
  onClose: () => void;
  t: ReturnType<typeof useT>;
}) {
  const [query, setQuery] = useState("");
  const eligible = squad.filter((p) => p.position === slot.position);
  const filtered = eligible.filter((p) =>
    p.playerName.toLowerCase().includes(query.toLowerCase()),
  );
  return (
    <BottomSheet title={`${t("league.lineup.pickPlayer")} · ${slot.position}`} onClose={onClose}>
      <div className="px-1 pt-2 pb-3">
        <div className="flex items-center gap-2 px-3 py-2 bg-zinc-100 rounded-lg">
          <Search className="h-4 w-4 text-zinc-500" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={t("league.lineup.pickPlayer")}
            className="flex-1 bg-transparent outline-none text-sm"
          />
        </div>
        {currentPlayerId && (
          <button
            onClick={onClear}
            className="mt-2 inline-flex items-center gap-1.5 text-xs font-bold text-rose-700 bg-rose-50 px-2.5 py-1.5 rounded-full"
          >
            <Trash2 className="h-3 w-3" />
            {t("league.lineup.empty")}
          </button>
        )}
      </div>
      <div className="flex flex-col gap-1.5">
        {filtered.length === 0 && (
          <div className="text-center text-sm text-zinc-500 py-8">
            {t("league.team.empty")}
          </div>
        )}
        {filtered.map((p) => {
          const img = resolvePlayerImage(p.imageRef, p.playerName);
          const used = alreadyInLineup.has(p.playerId) && p.playerId !== currentPlayerId;
          const isCurrent = p.playerId === currentPlayerId;
          return (
            <button
              key={p.playerId}
              onClick={() => !used && onPick(p.playerId)}
              disabled={used}
              className={`flex items-center gap-3 p-2 rounded-xl text-left transition ${
                isCurrent ? "bg-blue-50 border border-blue-600"
                : used ? "opacity-40 cursor-not-allowed"
                : "hover:bg-zinc-50"
              }`}
            >
              <div
                className="w-12 h-12 rounded-xl overflow-hidden flex items-center justify-center text-white font-bold text-sm flex-shrink-0"
                style={{ background: img.showImage ? undefined : getAvatarColor(p.playerName) }}
              >
                {img.showImage ? (
                  <img src={img.src!} alt={p.playerName} className="w-12 h-12 object-cover" />
                ) : (
                  img.initials
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div className="font-semibold text-zinc-900 truncate text-sm">{p.playerName}</div>
                <div className="text-[11px] text-zinc-500 truncate">{p.nationalTeam}</div>
              </div>
              <div className={`text-[10px] font-bold uppercase tracking-wider px-1.5 py-0.5 rounded ${POS_COLORS[p.position].chip}`}>
                {p.position}
              </div>
            </button>
          );
        })}
      </div>
    </BottomSheet>
  );
}
