"use client";

import { use, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ChevronLeft } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import {
  getLeague,
  getMyMembership,
  updateLeagueSettings,
  type LeagueRules,
} from "@/shared/api/leagueApi";
import { useT } from "@/shared/i18n/I18nProvider";

interface Props {
  params: Promise<{ leagueId: string }>;
}

// Starting budget and initial-squad shape are NOT editable: the standard
// FantasyNations start (15 random players + remaining money up to 200M) is
// mandatory.
const schema = z.object({
  moneyPerPoint: z.number().nonnegative(),
  releaseClauseProtectionHours: z.number().int().nonnegative(),
  marketRefreshIntervalHours: z.number().int().positive(),
  marketPlayersCount: z.number().int().positive(),
  maxPlayersPerSquad: z.number().int().positive(),
  minLineupPlayers: z.number().int().positive(),
  formationRulesEnabled: z.boolean(),
});

type FormData = z.infer<typeof schema>;

export default function LeagueSettingsPage({ params }: Props) {
  const { leagueId } = use(params);
  const router = useRouter();
  const queryClient = useQueryClient();
  const t = useT();

  const { data: league, isLoading: leagueLoading } = useQuery({
    queryKey: ["league", leagueId],
    queryFn: () => getLeague(leagueId),
  });

  const { data: me, isLoading: meLoading } = useQuery({
    queryKey: ["membership", leagueId],
    queryFn: () => getMyMembership(leagueId),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (league) {
      reset({
        moneyPerPoint: league.rules.moneyPerPoint,
        releaseClauseProtectionHours: league.rules.releaseClauseProtectionHours,
        marketRefreshIntervalHours: league.rules.marketRefreshIntervalHours,
        marketPlayersCount: league.rules.marketPlayersCount,
        maxPlayersPerSquad: league.rules.maxPlayersPerSquad,
        minLineupPlayers: league.rules.minLineupPlayers,
        formationRulesEnabled: league.rules.formationRulesEnabled,
      });
    }
  }, [league, reset]);

  const isOwner = !!me && me.role === "OWNER";

  async function onSubmit(values: FormData) {
    try {
      // Server preserves startingBudget + initial-squad shape regardless of what we send.
      const updated = await updateLeagueSettings(leagueId, {
        ...league!.rules,
        ...values,
      } as LeagueRules);
      queryClient.setQueryData(["league", leagueId], updated);
      queryClient.invalidateQueries({ queryKey: ["activity", leagueId] });
      toast.success("Settings saved.");
    } catch (e: unknown) {
      const msg =
        (e as { response?: { data?: { error?: string } } })?.response?.data?.error ||
        "Could not save settings.";
      toast.error(msg);
    }
  }

  if (leagueLoading || meLoading) {
    return (
      <div className="flex justify-center py-16">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500" />
      </div>
    );
  }

  return (
    <div className="p-4 max-w-lg mx-auto">
      <div className="flex items-center gap-2 mb-4">
        <Link
          href={`/leagues/${leagueId}/ranking`}
          className="text-zinc-500 hover:text-zinc-900 transition-colors"
          aria-label={t("common.back")}
        >
          <ChevronLeft className="h-5 w-5" />
        </Link>
        <h2 className="text-xl font-bold text-zinc-900 tracking-tight">{t("league.settings.title")}</h2>
      </div>

      {!isOwner ? (
        <div className="bg-white border border-zinc-200 shadow-sm rounded-xl p-5 text-sm text-zinc-600">
          Only the league owner can change these settings. Ask{" "}
          <span className="font-medium text-zinc-900">{league?.ownerNickname}</span> to update
          them.
        </div>
      ) : (
        <form
          onSubmit={handleSubmit(onSubmit)}
          className="bg-white border border-zinc-200 shadow-sm rounded-xl p-5 space-y-5"
        >
          {/* Standard FantasyNations start: budget + squad shape are fixed. */}
          <div className="text-xs text-zinc-500 border border-zinc-200 rounded-lg px-3 py-2 bg-zinc-50">
            Total budget per user: €{league!.rules.startingBudget.toLocaleString("en")} —
            15 random players + remaining money. Not configurable.
          </div>

          <SettingField
            label="Money per point (€)"
            help="Money each user earns per fantasy point scored."
            error={errors.moneyPerPoint?.message}
          >
            <Input
              type="number"
              step="1"
              min="0"
              className="bg-white border-zinc-300 tabular-nums"
              {...register("moneyPerPoint", { valueAsNumber: true })}
            />
          </SettingField>

          <SettingField
            label="Release-clause protection (hours)"
            help="How long a player is shielded from clause buys after being acquired."
            error={errors.releaseClauseProtectionHours?.message}
          >
            <Input
              type="number"
              step="1"
              min="0"
              className="bg-white border-zinc-300 tabular-nums"
              {...register("releaseClauseProtectionHours", { valueAsNumber: true })}
            />
          </SettingField>

          <SettingField
            label="Market refresh interval (hours)"
            help="How often the market is refreshed for this league. 24 is the default."
            error={errors.marketRefreshIntervalHours?.message}
          >
            <Input
              type="number"
              step="1"
              min="1"
              className="bg-white border-zinc-300 tabular-nums"
              {...register("marketRefreshIntervalHours", { valueAsNumber: true })}
            />
          </SettingField>

          <SettingField
            label="Players shown in market"
            help="How many players appear in each market refresh."
            error={errors.marketPlayersCount?.message}
          >
            <Input
              type="number"
              step="1"
              min="1"
              className="bg-white border-zinc-300 tabular-nums"
              {...register("marketPlayersCount", { valueAsNumber: true })}
            />
          </SettingField>

          <SettingField
            label="Max players per squad"
            help="Cap on the number of players a user can own."
            error={errors.maxPlayersPerSquad?.message}
          >
            <Input
              type="number"
              step="1"
              min="1"
              className="bg-white border-zinc-300 tabular-nums"
              {...register("maxPlayersPerSquad", { valueAsNumber: true })}
            />
          </SettingField>

          <SettingField
            label="Lineup size"
            help="Number of players required in a saved lineup."
            error={errors.minLineupPlayers?.message}
          >
            <Input
              type="number"
              step="1"
              min="1"
              className="bg-white border-zinc-300 tabular-nums"
              {...register("minLineupPlayers", { valueAsNumber: true })}
            />
          </SettingField>

          <div className="flex items-start gap-3 pt-1">
            <input
              id="formationRulesEnabled"
              type="checkbox"
              className="mt-1 h-4 w-4 rounded border-zinc-300 text-blue-600 focus:ring-blue-500"
              {...register("formationRulesEnabled")}
            />
            <label htmlFor="formationRulesEnabled" className="text-sm">
              <span className="font-medium text-zinc-900">Enforce formation rules</span>
              <span className="block text-xs text-zinc-500">
                When on, the lineup must respect position counts per formation. Otherwise any 11
                owned players (one per slot) are accepted.
              </span>
            </label>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => league && reset(league.rules)}
              disabled={isSubmitting || !isDirty}
              className="border-zinc-300 text-zinc-700"
            >
              Discard
            </Button>
            <Button
              type="submit"
              disabled={isSubmitting || !isDirty}
              className="bg-blue-600 hover:bg-blue-700"
            >
              {isSubmitting ? "Saving…" : "Save settings"}
            </Button>
          </div>
        </form>
      )}
    </div>
  );
}

function SettingField({
  label,
  help,
  error,
  children,
}: {
  label: string;
  help?: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="space-y-1.5">
      <Label className="text-zinc-900">{label}</Label>
      {children}
      {help && !error && <p className="text-[11px] text-zinc-500">{help}</p>}
      {error && <p className="text-[11px] text-rose-600">{error}</p>}
    </div>
  );
}
