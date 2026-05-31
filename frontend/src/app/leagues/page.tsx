"use client";

import { useState, useEffect } from "react";
import { Plus, UserPlus, Trophy, LogOut } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useUserLeagues } from "./hooks/useUserLeagues";
import { LeagueCard } from "./components/LeagueCard";
import { CreateLeagueModal } from "./components/CreateLeagueModal";
import { JoinLeagueModal } from "./components/JoinLeagueModal";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { getInitials, getAvatarColor } from "@/shared/assets/fallbackAvatar";
import { useRouter } from "next/navigation";
import { useT } from "@/shared/i18n/I18nProvider";
import { LanguageSelector } from "@/shared/i18n/LanguageSelector";

export default function LeaguesPage() {
  const { data: leagues, isLoading } = useUserLeagues();
  const { user, logout, hydrate } = useCurrentUser();
  const router = useRouter();
  const [createOpen, setCreateOpen] = useState(false);
  const [joinOpen, setJoinOpen] = useState(false);
  const t = useT();

  useEffect(() => {
    hydrate();
  }, [hydrate]);

  useEffect(() => {
    if (!user && !isLoading) {
      router.push("/");
    }
  }, [user, isLoading, router]);

  function handleLogout() {
    logout();
    router.push("/");
  }

  return (
    <div className="min-h-screen bg-zinc-50">
      <header className="sticky top-0 z-30 bg-white/90 backdrop-blur border-b border-zinc-200">
        <div className="max-w-4xl mx-auto px-4 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Trophy className="h-6 w-6 text-amber-500" />
            <span className="font-bold text-zinc-900">Fantasy Nations</span>
          </div>
          <div className="flex items-center gap-3">
            <LanguageSelector />
            {user && (
              <>
                <span className="text-sm text-zinc-700 hidden sm:inline">
                  {user.nickname}
                </span>
                <div
                  className="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold text-white overflow-hidden"
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
              </>
            )}
            <button
              onClick={handleLogout}
              className="text-zinc-500 hover:text-zinc-900 transition-colors"
              aria-label={t("leagues.logout")}
            >
              <LogOut className="h-5 w-5" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold text-zinc-900">{t("leagues.myLeagues")}</h1>
          <div className="flex gap-2">
            <Button
              onClick={() => setJoinOpen(true)}
              variant="outline"
              size="sm"
              className="border-zinc-300 text-zinc-700 hover:text-zinc-900 gap-1"
            >
              <UserPlus className="h-4 w-4" /> {t("leagues.join")}
            </Button>
            <Button
              onClick={() => setCreateOpen(true)}
              size="sm"
              className="bg-blue-600 hover:bg-blue-700 gap-1"
            >
              <Plus className="h-4 w-4" /> {t("leagues.create")}
            </Button>
          </div>
        </div>

        {isLoading ? (
          <div className="flex justify-center py-16">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500" />
          </div>
        ) : leagues && leagues.length > 0 ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {leagues.map((league) => (
              <LeagueCard key={league.id} league={league} />
            ))}
          </div>
        ) : (
          <div className="text-center py-16">
            <Trophy className="h-16 w-16 text-zinc-300 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-zinc-700 mb-2">{t("leagues.noneYet")}</h2>
            <p className="text-zinc-500 mb-6">{t("leagues.emptyHint")}</p>
            <div className="flex gap-3 justify-center">
              <Button
                onClick={() => setJoinOpen(true)}
                variant="outline"
                className="border-zinc-300 text-zinc-700"
              >
                {t("leagues.joinWithCode")}
              </Button>
              <Button
                onClick={() => setCreateOpen(true)}
                className="bg-blue-600 hover:bg-blue-700"
              >
                {t("leagues.createLeague")}
              </Button>
            </div>
          </div>
        )}
      </main>

      <CreateLeagueModal open={createOpen} onClose={() => setCreateOpen(false)} />
      <JoinLeagueModal open={joinOpen} onClose={() => setJoinOpen(false)} />
    </div>
  );
}
