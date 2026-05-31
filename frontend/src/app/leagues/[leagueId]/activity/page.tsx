"use client";

import { use } from "react";
import { useQuery } from "@tanstack/react-query";
import { getActivity } from "@/shared/api/activityApi";
import {
  ShoppingCart,
  Users,
  RefreshCw,
  Activity,
  AlertCircle,
} from "lucide-react";
import { useT } from "@/shared/i18n/I18nProvider";

interface Props {
  params: Promise<{ leagueId: string }>;
}

const eventIcons: Record<string, React.ReactNode> = {
  PLAYER_BOUGHT: <ShoppingCart className="h-4 w-4 text-emerald-600" />,
  CLAUSE_PAID: <AlertCircle className="h-4 w-4 text-rose-600" />,
  USER_JOINED: <Users className="h-4 w-4 text-blue-600" />,
  MARKET_REFRESHED: <RefreshCw className="h-4 w-4 text-zinc-500" />,
  LINEUP_CHANGED: <Activity className="h-4 w-4 text-purple-600" />,
  LEAGUE_CREATED: <Activity className="h-4 w-4 text-amber-500" />,
  RULES_CHANGED: <Activity className="h-4 w-4 text-orange-600" />,
  RELEASE_CLAUSE_CHANGED: <AlertCircle className="h-4 w-4 text-indigo-600" />,
  PLAYER_SOLD: <ShoppingCart className="h-4 w-4 text-yellow-600" />,
};

type ActivityEntry = { userNickname: string; payload: Record<string, unknown> };
type TFn = ReturnType<typeof useT>;

function labelFor(t: TFn, eventType: string, entry: ActivityEntry): string {
  const vars = {
    user: entry.userNickname,
    nickname: entry.userNickname,
    playerName: String(entry.payload.playerName ?? ""),
    price: String(entry.payload.price ?? ""),
    leagueName: String(entry.payload.leagueName ?? ""),
  };
  switch (eventType) {
    case "PLAYER_BOUGHT":
    case "USER_JOINED":
    case "MARKET_REFRESHED":
    case "LEAGUE_CREATED":
    case "RULES_CHANGED":
    case "PLAYER_SOLD":
      return t(`league.activity.${eventType}` as Parameters<typeof t>[0], vars);
    default:
      return eventType;
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function ActivityPage({ params }: Props) {
  const { leagueId } = use(params);
  const t = useT();
  const { data: activity, isLoading } = useQuery({
    queryKey: ["activity", leagueId],
    queryFn: () => getActivity(leagueId),
  });

  return (
    <div className="p-4 max-w-lg mx-auto">
      <h2 className="text-xl font-bold text-zinc-900 mb-4">{t("league.activity.title")}</h2>
      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500" />
        </div>
      ) : activity && activity.length > 0 ? (
        <div className="space-y-2">
          {activity.map((entry) => {
            const label = labelFor(t, entry.eventType, entry);
            return (
              <div
                key={entry.id}
                className="bg-white border border-zinc-200 shadow-sm rounded-xl px-4 py-3 flex items-center gap-3"
              >
                <div className="flex-shrink-0">
                  {eventIcons[entry.eventType] ?? (
                    <Activity className="h-4 w-4 text-zinc-500" />
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-zinc-900 truncate">{label}</p>
                  <p className="text-xs text-zinc-500">{formatDate(entry.createdAt)}</p>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <div className="text-center py-12 text-zinc-500">
          {t("league.activity.empty")}
        </div>
      )}
    </div>
  );
}
