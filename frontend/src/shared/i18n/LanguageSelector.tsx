"use client";

import { Languages } from "lucide-react";
import { useI18n, type Lang } from "./I18nProvider";

interface LanguageSelectorProps {
  variant?: "compact" | "full";
  className?: string;
}

const ORDER: Lang[] = ["en", "es", "gl"];

export function LanguageSelector({ variant = "compact", className = "" }: LanguageSelectorProps) {
  const { lang, setLang, t } = useI18n();

  return (
    <label className={`inline-flex items-center gap-2 ${className}`}>
      <Languages className="h-4 w-4 text-zinc-500" aria-hidden />
      {variant === "full" && (
        <span className="text-sm text-zinc-700">{t("language.label")}:</span>
      )}
      <select
        value={lang}
        onChange={(e) => setLang(e.target.value as Lang)}
        aria-label={t("language.label")}
        className="text-sm bg-white border border-zinc-300 rounded-md px-2 py-1 focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        {ORDER.map((code) => (
          <option key={code} value={code}>
            {t(`language.${code}` as const)}
          </option>
        ))}
      </select>
    </label>
  );
}
