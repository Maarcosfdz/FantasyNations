"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { register as registerUser } from "./authClient";
import { useCurrentUser } from "./useCurrentUser";
import { toast } from "sonner";
import { useT } from "@/shared/i18n/I18nProvider";

const schema = z.object({
  email: z.string().email("invalidEmail"),
  nickname: z.string().min(3, "nicknameMin").max(64, "nicknameMax"),
  password: z.string().min(8, "passwordMin"),
  confirmPassword: z.string(),
}).refine((d) => d.password === d.confirmPassword, {
  message: "passwordsDontMatch",
  path: ["confirmPassword"],
});

type FormData = z.infer<typeof schema>;

interface RegisterFormProps {
  onSuccess: () => void;
  onLogin: () => void;
}

export function RegisterForm({ onSuccess, onLogin }: RegisterFormProps) {
  const setUser = useCurrentUser((s) => s.setUser);
  const t = useT();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  function fieldError(code?: string): string | undefined {
    if (!code) return undefined;
    return t(`auth.${code}` as Parameters<typeof t>[0]);
  }

  async function onSubmit(data: FormData) {
    try {
      const res = await registerUser(data.email, data.nickname, data.password);
      setUser({ userId: res.userId, email: res.email, nickname: res.nickname, avatarUrl: res.avatarUrl });
      onSuccess();
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { error?: string } } })?.response?.data?.error;
      toast.error(msg ?? t("auth.registrationFailed"));
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="space-y-1">
        <Label htmlFor="email">{t("auth.email")}</Label>
        <Input id="email" type="email" placeholder="you@example.com" {...register("email")} className="bg-white border-zinc-300" />
        {errors.email && <p className="text-rose-600 text-xs">{fieldError(errors.email.message)}</p>}
      </div>
      <div className="space-y-1">
        <Label htmlFor="nickname">{t("auth.nickname")}</Label>
        <Input id="nickname" {...register("nickname")} className="bg-white border-zinc-300" />
        {errors.nickname && <p className="text-rose-600 text-xs">{fieldError(errors.nickname.message)}</p>}
      </div>
      <div className="space-y-1">
        <Label htmlFor="password">{t("auth.password")}</Label>
        <Input id="password" type="password" placeholder="••••••••" {...register("password")} className="bg-white border-zinc-300" />
        {errors.password && <p className="text-rose-600 text-xs">{fieldError(errors.password.message)}</p>}
      </div>
      <div className="space-y-1">
        <Label htmlFor="confirmPassword">{t("auth.confirmPassword")}</Label>
        <Input id="confirmPassword" type="password" placeholder="••••••••" {...register("confirmPassword")} className="bg-white border-zinc-300" />
        {errors.confirmPassword && <p className="text-rose-600 text-xs">{fieldError(errors.confirmPassword.message)}</p>}
      </div>
      <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700" disabled={isSubmitting}>
        {isSubmitting ? t("auth.registering") : t("auth.createAccount")}
      </Button>
      <p className="text-center text-sm text-zinc-500">
        {t("auth.haveAccount")}{" "}
        <button type="button" onClick={onLogin} className="text-blue-600 hover:underline">
          {t("auth.signIn")}
        </button>
      </p>
    </form>
  );
}
