"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { login } from "./authClient";
import { useCurrentUser } from "./useCurrentUser";
import { toast } from "sonner";
import { useT } from "@/shared/i18n/I18nProvider";

const schema = z.object({
  email: z.string().email("invalidEmail"),
  password: z.string().min(1, "passwordRequired"),
});

type FormData = z.infer<typeof schema>;

interface LoginFormProps {
  onSuccess: () => void;
  onForgotPassword: () => void;
  onRegister: () => void;
}

export function LoginForm({ onSuccess, onForgotPassword, onRegister }: LoginFormProps) {
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
      const res = await login(data.email, data.password);
      setUser({ userId: res.userId, email: res.email, nickname: res.nickname, avatarUrl: res.avatarUrl });
      onSuccess();
    } catch {
      toast.error(t("auth.invalidCredentials"));
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div className="space-y-1">
        <Label htmlFor="email">{t("auth.email")}</Label>
        <Input
          id="email"
          type="email"
          placeholder="you@example.com"
          {...register("email")}
          className="bg-white border-zinc-300"
        />
        {errors.email && <p className="text-rose-600 text-xs">{fieldError(errors.email.message)}</p>}
      </div>
      <div className="space-y-1">
        <Label htmlFor="password">{t("auth.password")}</Label>
        <Input
          id="password"
          type="password"
          placeholder="••••••••"
          {...register("password")}
          className="bg-white border-zinc-300"
        />
        {errors.password && <p className="text-rose-600 text-xs">{fieldError(errors.password.message)}</p>}
      </div>
      <div className="text-right">
        <button
          type="button"
          onClick={onForgotPassword}
          className="text-xs text-blue-600 hover:underline"
        >
          {t("auth.forgotPassword")}
        </button>
      </div>
      <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700" disabled={isSubmitting}>
        {isSubmitting ? t("auth.signingIn") : t("auth.signIn")}
      </Button>
      <p className="text-center text-sm text-zinc-500">
        {t("auth.noAccount")}{" "}
        <button type="button" onClick={onRegister} className="text-blue-600 hover:underline">
          {t("auth.register")}
        </button>
      </p>
    </form>
  );
}
