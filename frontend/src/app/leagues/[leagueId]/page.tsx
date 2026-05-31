import { redirect } from "next/navigation";

interface Props {
  params: Promise<{ leagueId: string }>;
}

export default async function LeaguePage({ params }: Props) {
  const { leagueId } = await params;
  redirect(`/leagues/${leagueId}/ranking`);
}
