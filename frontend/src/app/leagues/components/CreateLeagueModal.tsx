"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { createLeague } from "@/shared/api/leagueApi";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useRouter } from "next/navigation";
import { useT } from "@/shared/i18n/I18nProvider";

const schema = z.object({
  name: z.string().min(2).max(128),
});

type FormData = z.infer<typeof schema>;

interface CreateLeagueModalProps {
  open: boolean;
  onClose: () => void;
}

export function CreateLeagueModal({ open, onClose }: CreateLeagueModalProps) {
  const queryClient = useQueryClient();
  const router = useRouter();
  const t = useT();
  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } =
    useForm<FormData>({ resolver: zodResolver(schema) });

  async function onSubmit(data: FormData) {
    try {
      const league = await createLeague({ name: data.name });
      queryClient.invalidateQueries({ queryKey: ["leagues"] });
      toast.success(t("leagues.createdToast"));
      reset();
      onClose();
      router.push(`/leagues/${league.id}/ranking`);
    } catch {
      toast.error(t("common.error"));
    }
  }

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) { reset(); onClose(); } }}>
      <DialogContent className="bg-white border-zinc-200 text-zinc-900 max-w-md">
        <DialogHeader>
          <DialogTitle>{t("leagues.createTitle")}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-1">
            <Label>{t("common.name")}</Label>
            <Input
              {...register("name")}
              placeholder={t("leagues.createNamePlaceholder")}
              className="bg-white border-zinc-300"
            />
            {errors.name && <p className="text-rose-600 text-xs">{t("auth.nicknameMin")}</p>}
          </div>
          <p className="text-xs text-zinc-500">
            {t("leagues.standardStartHint")}
          </p>
          <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700" disabled={isSubmitting}>
            {isSubmitting ? t("common.saving") : t("leagues.createButton")}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
