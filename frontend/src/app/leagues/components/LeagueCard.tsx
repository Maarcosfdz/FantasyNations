"use client";

import { Users, Trophy } from "lucide-react";
import { useRouter } from "next/navigation";
import type { League } from "@/shared/api/leagueApi";
import { useT } from "@/shared/i18n/I18nProvider";

interface LeagueCardProps {
  league: League;
}

export function LeagueCard({ league }: LeagueCardProps) {
  const router = useRouter();
  const t = useT();

  return (
    <button
      onClick={() => router.push(`/leagues/${league.id}/ranking`)}
      className="w-full text-left bg-white hover:shadow-md hover:-translate-y-0.5 hover:border-blue-300 border border-zinc-200 rounded-2xl p-5 shadow-sm transition-all group"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center">
          <Trophy className="h-5 w-5 text-amber-500" />
        </div>
        <span className="text-xs text-zinc-500 font-mono">{league.inviteCode}</span>
      </div>
      <h3 className="font-bold text-zinc-900 text-lg mb-1 group-hover:text-blue-700 transition-colors tracking-tight">
        {league.name}
      </h3>
      <div className="flex items-center gap-2 text-zinc-500 text-sm">
        <Users className="h-4 w-4" />
        <span>{league.memberCount} {league.memberCount === 1 ? t("leagues.member") : t("leagues.members")}</span>
      </div>
    </button>
  );
}
