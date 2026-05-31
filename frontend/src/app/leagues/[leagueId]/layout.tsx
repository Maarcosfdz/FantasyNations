import { LeagueBottomNav } from "./components/LeagueBottomNav";
import { LeagueHeader } from "./components/LeagueHeader";

interface LeagueLayoutProps {
  children: React.ReactNode;
  params: Promise<{ leagueId: string }>;
}

export default async function LeagueLayout({ children, params }: LeagueLayoutProps) {
  const { leagueId } = await params;

  return (
    <div className="min-h-screen bg-zinc-50 flex flex-col">
      <LeagueHeader leagueId={leagueId} />
      <main className="flex-1 pb-20 overflow-y-auto">{children}</main>
      <LeagueBottomNav leagueId={leagueId} />
    </div>
  );
}
