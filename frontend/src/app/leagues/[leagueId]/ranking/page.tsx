"use client";

import { use } from "react";
import { useQuery } from "@tanstack/react-query";
import { getRanking } from "@/shared/api/rankingApi";
import { getInitials, getAvatarColor } from "@/shared/assets/fallbackAvatar";
import { Trophy } from "lucide-react";
import { useT } from "@/shared/i18n/I18nProvider";

interface Props {
  params: Promise<{ leagueId: string }>;
}

export default function RankingPage({ params }: Props) {
  const { leagueId } = use(params);
  const t = useT();
  const { data: ranking, isLoading } = useQuery({
    queryKey: ["ranking", leagueId],
    queryFn: () => getRanking(leagueId),
  });

  const medalColors = ["text-amber-500", "text-zinc-400", "text-amber-700"];

  return (
    <div className="p-4 max-w-lg mx-auto">
      <h2 className="text-xl font-bold text-zinc-900 mb-4">{t("league.ranking.title")}</h2>
      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500" />
        </div>
      ) : (
        <div className="space-y-2">
          {ranking?.map((entry) => (
            <div
              key={entry.userId}
              className="bg-white border border-zinc-200 shadow-sm rounded-xl px-4 py-3 flex items-center gap-3"
            >
              <span
                className={`text-lg font-bold w-7 text-center tabular-nums ${
                  medalColors[entry.rank - 1] ?? "text-zinc-500"
                }`}
              >
                {entry.rank <= 3 ? <Trophy className="h-5 w-5 inline" /> : entry.rank}
              </span>
              <div
                className="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold text-white flex-shrink-0 overflow-hidden"
                style={{ background: getAvatarColor(entry.nickname) }}
              >
                {entry.avatarUrl ? (
                  <img
                    src={entry.avatarUrl}
                    alt={entry.nickname}
                    className="w-9 h-9 rounded-full object-cover"
                  />
                ) : (
                  getInitials(entry.nickname)
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div className="font-medium text-zinc-900 truncate">{entry.nickname}</div>
                <div className="text-[11px] text-emerald-700 tabular-nums">
                  {t("league.ranking.squadValue")}:{" "}
                  {new Intl.NumberFormat("en", {
                    style: "currency",
                    currency: "EUR",
                    notation: "compact",
                    maximumFractionDigits: 1,
                  }).format(entry.squadValue)}
                </div>
              </div>
              <span className="text-blue-600 font-bold tabular-nums">
                {entry.totalPoints} {t("league.ranking.points")}
              </span>
            </div>
          ))}
          {ranking?.length === 0 && (
            <div className="text-center py-12 text-zinc-500">{t("league.ranking.noMembers")}</div>
          )}
        </div>
      )}
    </div>
  );
}
