"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { joinLeague } from "@/shared/api/leagueApi";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import { useT } from "@/shared/i18n/I18nProvider";

const schema = z.object({
  inviteCode: z.string().min(6),
});

type FormData = z.infer<typeof schema>;

interface JoinLeagueModalProps {
  open: boolean;
  onClose: () => void;
}

export function JoinLeagueModal({ open, onClose }: JoinLeagueModalProps) {
  const queryClient = useQueryClient();
  const router = useRouter();
  const t = useT();
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } =
    useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    try {
      const league = await joinLeague(data.inviteCode.toUpperCase());
      queryClient.invalidateQueries({ queryKey: ["leagues"] });
      toast.success(t("leagues.joinedToast"));
      reset();
      onClose();
      router.push(`/leagues/${league.id}/ranking`);
    } catch {
      toast.error(t("leagues.invalidInviteCode"));
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) { reset(); onClose(); } }}>
      <DialogContent className="bg-white border-zinc-200 text-zinc-900 max-w-md">
        <DialogHeader>
          <DialogTitle>{t("leagues.joinTitle")}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1">
            <Label>{t("league.settings.inviteCode")}</Label>
            <Input
              {...register("inviteCode")}
              placeholder={t("leagues.joinCodePlaceholder")}
              className="bg-white border-zinc-300 uppercase tracking-widest font-mono"
            />
            {errors.inviteCode && <p className="text-rose-600 text-xs">{t("leagues.invalidInviteCode")}</p>}
          </div>
          <Button type="submit" className="w-full bg-green-600 hover:bg-green-700" disabled={isSubmitting}>
            {isSubmitting ? t("common.saving") : t("leagues.joinButton")}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
