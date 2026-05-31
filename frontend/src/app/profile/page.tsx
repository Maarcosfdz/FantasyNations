"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCurrentUser } from "@/shared/auth/useCurrentUser";
import { getInitials, getAvatarColor } from "@/shared/assets/fallbackAvatar";
import { ChevronLeft } from "lucide-react";
import Link from "next/link";
import apiClient from "@/shared/api/apiClient";
import { toast } from "sonner";
import { useT } from "@/shared/i18n/I18nProvider";
import { LanguageSelector } from "@/shared/i18n/LanguageSelector";

const schema = z.object({
  nickname: z.string().min(3, "nicknameMin").max(64, "nicknameMax"),
});

type FormData = z.infer<typeof schema>;

export default function ProfilePage() {
  const { user, setUser, hydrate } = useCurrentUser();
  const router = useRouter();
  const t = useT();

  useEffect(() => { hydrate(); }, [hydrate]);
  useEffect(() => { if (!user) router.push("/"); }, [user, router]);

  const { register, handleSubmit, formState: { errors, isSubmitting } } =
    useForm<FormData>({
      resolver: zodResolver(schema),
      defaultValues: { nickname: user?.nickname ?? "" },
    });

  function fieldError(code?: string): string | undefined {
    if (!code) return undefined;
    return t(`auth.${code}` as Parameters<typeof t>[0]);
  }

  async function onSubmit(data: FormData) {
    try {
      const { data: updated } = await apiClient.patch("/api/users/me", { nickname: data.nickname });
      setUser({ ...user!, nickname: updated.nickname });
      localStorage.setItem("fn_user", JSON.stringify({ ...user, nickname: updated.nickname }));
      toast.success(t("profile.saved"));
    } catch {
      toast.error(t("profile.saveFailed"));
    }
  }

  if (!user) return null;

  return (
    <div className="min-h-screen bg-zinc-50 p-4">
      <div className="max-w-md mx-auto">
        <div className="flex items-center gap-2 mb-6">
          <Link href="/leagues" className="text-zinc-500 hover:text-zinc-900 transition-colors">
            <ChevronLeft className="h-5 w-5" />
          </Link>
          <h1 className="text-xl font-bold text-zinc-900 tracking-tight">{t("profile.title")}</h1>
        </div>

        <div className="flex flex-col items-center mb-8">
          <div
            className="w-20 h-20 rounded-full flex items-center justify-center text-2xl font-bold text-white mb-3"
            style={{ background: getAvatarColor(user.nickname) }}
          >
            {getInitials(user.nickname)}
          </div>
          <p className="text-zinc-500 text-sm">{user.email}</p>
        </div>

        <div className="bg-white rounded-2xl p-6 border border-zinc-200 shadow-sm space-y-6">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-1">
              <Label>{t("profile.nickname")}</Label>
              <Input
                {...register("nickname")}
                className="bg-white border-zinc-300"
                defaultValue={user.nickname}
              />
              {errors.nickname && <p className="text-rose-600 text-xs">{fieldError(errors.nickname.message)}</p>}
            </div>
            <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700" disabled={isSubmitting}>
              {isSubmitting ? t("profile.saving") : t("profile.save")}
            </Button>
          </form>

          <div className="pt-4 border-t border-zinc-200">
            <Label className="block mb-2">{t("profile.language")}</Label>
            <LanguageSelector variant="full" />
          </div>
        </div>
      </div>
    </div>
  );
}
