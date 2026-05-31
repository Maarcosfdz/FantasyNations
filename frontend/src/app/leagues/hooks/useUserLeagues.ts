import { useQuery } from "@tanstack/react-query";
import { getUserLeagues } from "@/shared/api/leagueApi";

export function useUserLeagues() {
  return useQuery({
    queryKey: ["leagues"],
    queryFn: getUserLeagues,
  });
}
