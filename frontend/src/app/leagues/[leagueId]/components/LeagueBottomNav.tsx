"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Users, LayoutGrid, Trophy, ShoppingCart, Activity } from "lucide-react";
import { useT } from "@/shared/i18n/I18nProvider";

interface LeagueBottomNavProps {
  leagueId: string;
}

const tabs = [
  { key: "team", icon: Users, path: "team" },
  { key: "lineup", icon: LayoutGrid, path: "lineup" },
  { key: "ranking", icon: Trophy, path: "ranking" },
  { key: "market", icon: ShoppingCart, path: "market" },
  { key: "activity", icon: Activity, path: "activity" },
] as const;

export function LeagueBottomNav({ leagueId }: LeagueBottomNavProps) {
  const pathname = usePathname();
  const t = useT();

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur border-t border-zinc-200">
      <div className="max-w-lg mx-auto flex">
        {tabs.map((tab) => {
          const href = `/leagues/${leagueId}/${tab.path}`;
          const active = pathname.startsWith(href);
          return (
            <Link
              key={tab.path}
              href={href}
              className={`flex-1 flex flex-col items-center justify-center py-3 gap-1 text-xs transition-colors ${
                active
                  ? "text-blue-600 border-t-2 border-blue-600 -mt-0.5"
                  : "text-zinc-500 hover:text-zinc-900"
              }`}
            >
              <tab.icon className="h-5 w-5" />
              <span>{t(`league.tabs.${tab.key}` as const)}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
