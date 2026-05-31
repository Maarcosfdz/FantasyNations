"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { forgotPassword } from "./authClient";
import { toast } from "sonner";
import { useT } from "@/shared/i18n/I18nProvider";

const schema = z.object({
  email: z.string().email("invalidEmail"),
});

type FormData = z.infer<typeof schema>;

interface ForgotPasswordFormProps {
  onBack: () => void;
}

export function ForgotPasswordForm({ onBack }: ForgotPasswordFormProps) {
  const [sent, setSent] = useState(false);
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
      await forgotPassword(data.email);
      setSent(true);
    } catch {
      toast.error(t("common.error"));
    }
  }

  if (sent) {
    return (
      <div className="text-center space-y-4">
        <div className="text-4xl">📧</div>
        <p className="text-zinc-500 text-sm">{t("auth.resetSent")}</p>
        <Button variant="outline" onClick={onBack} className="border-zinc-300">
          {t("auth.backToSignIn")}
        </Button>
      </div>
    );
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
      <Button type="submit" className="w-full bg-blue-600 hover:bg-blue-700" disabled={isSubmitting}>
        {isSubmitting ? t("auth.sending") : t("auth.sendResetLink")}
      </Button>
      <p className="text-center">
        <button type="button" onClick={onBack} className="text-sm text-zinc-500 hover:underline">
          {t("auth.backToSignIn")}
        </button>
      </p>
    </form>
  );
}
