"use client";

import { useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { usePathname } from "next/navigation";
import Link from "next/link";
import { ChevronLeft, Settings, Trophy } from "lucide-react";
import { getLeague, getMyMembership } from "@/shared/api/leagueApi";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { getInitials, getAvatarColor } from "@/shared/assets/fallbackAvatar";
import { useT } from "@/shared/i18n/I18nProvider";

interface LeagueHeaderProps {
  leagueId: string;
}

const TAB_KEYS = ["team", "lineup", "ranking", "market", "activity", "settings"] as const;
type TabKey = typeof TAB_KEYS[number];

export function LeagueHeader({ leagueId }: LeagueHeaderProps) {
  const pathname = usePathname();
  const { user, hydrate } = useCurrentUser();
  const t = useT();

  const segments = pathname.split("/").filter(Boolean);
  const tab = segments[2] ?? "";
  const pageLabel = (TAB_KEYS as readonly string[]).includes(tab)
    ? t(`league.tabs.${tab as TabKey}` as const)
    : "";

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  const { data: league } = useQuery({
    queryKey: ["league", leagueId],
    queryFn: () => getLeague(leagueId),
  });

  const { data: me } = useQuery({
    queryKey: ["membership", leagueId],
    queryFn: () => getMyMembership(leagueId),
  });

  const isOwner = me?.role === "OWNER";
  const onSettingsPage = pathname.endsWith("/settings");

  return (
    <header className="sticky top-0 z-30 bg-white/90 backdrop-blur border-b border-zinc-200">
      <div className="max-w-lg mx-auto px-4 py-3 flex items-center gap-3">
        <Link
          href="/leagues"
          className="text-zinc-500 hover:text-zinc-900 transition-colors"
          aria-label={t("league.backAria")}
        >
          <ChevronLeft className="h-5 w-5" />
        </Link>
        <Trophy className="h-5 w-5 text-amber-500" />
        <div className="flex-1 min-w-0">
          <div className="text-xs text-zinc-500 truncate">{league?.name ?? "…"}</div>
          <div className="text-sm font-semibold text-zinc-900 leading-tight">{pageLabel}</div>
        </div>
        {isOwner && !onSettingsPage && (
          <Link
            href={`/leagues/${leagueId}/settings`}
            className="text-zinc-500 hover:text-zinc-900 transition-colors"
            aria-label={t("league.settingsAria")}
          >
            <Settings className="h-5 w-5" />
          </Link>
        )}
        {user && (
          <div className="flex items-center gap-2">
            <span className="text-sm text-zinc-700 hidden sm:inline">{user.nickname}</span>
            <div
              className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-white"
              style={{ background: getAvatarColor(user.nickname) }}
              aria-label={user.nickname}
            >
              {user.avatarUrl ? (
                <img
                  src={user.avatarUrl}
                  alt={user.nickname}
                  className="w-8 h-8 rounded-full object-cover"
                />
              ) : (
                getInitials(user.nickname)
              )}
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
