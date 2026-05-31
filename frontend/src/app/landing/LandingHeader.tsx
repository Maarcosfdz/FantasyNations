"use client";

import { useState } from "react";
import { Trophy, Menu, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useT } from "@/shared/i18n/I18nProvider";
import { LanguageSelector } from "@/shared/i18n/LanguageSelector";

interface LandingHeaderProps {
  onLoginClick: () => void;
}

export function LandingHeader({ onLoginClick }: LandingHeaderProps) {
  const [mobileOpen, setMobileOpen] = useState(false);
  const t = useT();

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-zinc-200">
      <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Trophy className="h-6 w-6 text-amber-500" />
          <span className="font-bold text-lg text-zinc-900 tracking-tight">
            Fantasy Nations
          </span>
        </div>

        <nav className="hidden md:flex items-center gap-6">
          <a
            href="#home"
            className="text-zinc-600 hover:text-zinc-900 transition-colors text-sm"
          >
            {t("landing.nav.home")}
          </a>
          <a
            href="#about"
            className="text-zinc-600 hover:text-zinc-900 transition-colors text-sm"
          >
            {t("landing.nav.about")}
          </a>
          <LanguageSelector />
          <Button onClick={onLoginClick} size="sm" className="bg-blue-600 hover:bg-blue-700">
            {t("landing.nav.signIn")}
          </Button>
        </nav>

        <button
          className="md:hidden text-zinc-600 hover:text-zinc-900"
          onClick={() => setMobileOpen((v) => !v)}
          aria-label={t("landing.nav.toggleMenu")}
        >
          {mobileOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
        </button>
      </div>

      {mobileOpen && (
        <div className="md:hidden border-t border-zinc-200 bg-white px-4 py-4 flex flex-col gap-4">
          <a
            href="#home"
            className="text-zinc-700 hover:text-zinc-900"
            onClick={() => setMobileOpen(false)}
          >
            {t("landing.nav.home")}
          </a>
          <a
            href="#about"
            className="text-zinc-700 hover:text-zinc-900"
            onClick={() => setMobileOpen(false)}
          >
            {t("landing.nav.about")}
          </a>
          <LanguageSelector variant="full" />
          <Button
            onClick={() => {
              setMobileOpen(false);
              onLoginClick();
            }}
            className="bg-blue-600 hover:bg-blue-700 w-full"
          >
            {t("landing.nav.signIn")}
          </Button>
        </div>
      )}
    </header>
  );
}
