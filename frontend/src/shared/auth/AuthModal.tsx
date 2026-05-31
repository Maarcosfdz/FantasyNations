"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { LoginForm } from "./LoginForm";
import { RegisterForm } from "./RegisterForm";
import { ForgotPasswordForm } from "./ForgotPasswordForm";
import { useT } from "@/shared/i18n/I18nProvider";

type Mode = "login" | "register" | "forgot";

interface AuthModalProps {
  open: boolean;
  onClose: () => void;
}

export function AuthModal({ open, onClose }: AuthModalProps) {
  const [mode, setMode] = useState<Mode>("login");
  const router = useRouter();
  const t = useT();
  const titles: Record<Mode, string> = {
    login: t("auth.signIn"),
    register: t("auth.createAccount"),
    forgot: t("auth.resetPassword"),
  };

  function handleSuccess() {
    onClose();
    router.push("/leagues");
  }

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) onClose(); }}>
      <DialogContent className="bg-white border border-zinc-200 text-zinc-900 max-w-md w-full">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold text-zinc-900 tracking-tight">
            {titles[mode]}
          </DialogTitle>
        </DialogHeader>

        {mode === "login" && (
          <LoginForm
            onSuccess={handleSuccess}
            onForgotPassword={() => setMode("forgot")}
            onRegister={() => setMode("register")}
          />
        )}
        {mode === "register" && (
          <RegisterForm
            onSuccess={handleSuccess}
            onLogin={() => setMode("login")}
          />
        )}
        {mode === "forgot" && (
          <ForgotPasswordForm onBack={() => setMode("login")} />
        )}
      </DialogContent>
    </Dialog>
  );
}
