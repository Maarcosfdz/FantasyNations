"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { en, type Dictionary } from "./locales/en";
import { es } from "./locales/es";
import { gl } from "./locales/gl";

export type Lang = "en" | "es" | "gl";

const DICTS: Record<Lang, Dictionary> = { en, es, gl };
const STORAGE_KEY = "fn:lang";
const DEFAULT_LANG: Lang = "en";

type DotKeys<T, P extends string = ""> = {
  [K in keyof T & string]: T[K] extends Record<string, unknown>
    ? DotKeys<T[K], `${P}${K}.`>
    : `${P}${K}`;
}[keyof T & string];

export type TKey = DotKeys<Dictionary>;

interface I18nContextValue {
  lang: Lang;
  setLang: (l: Lang) => void;
  t: (key: TKey, vars?: Record<string, string | number>) => string;
}

const I18nContext = createContext<I18nContextValue | null>(null);

function lookup(dict: Dictionary, key: string): string {
  const parts = key.split(".");
  let cursor: unknown = dict;
  for (const part of parts) {
    if (cursor && typeof cursor === "object" && part in (cursor as Record<string, unknown>)) {
      cursor = (cursor as Record<string, unknown>)[part];
    } else {
      return key;
    }
  }
  return typeof cursor === "string" ? cursor : key;
}

function interpolate(template: string, vars?: Record<string, string | number>): string {
  if (!vars) return template;
  return template.replace(/\{(\w+)\}/g, (_, name) =>
    name in vars ? String(vars[name]) : `{${name}}`
  );
}

function readStoredLang(): Lang {
  if (typeof window === "undefined") return DEFAULT_LANG;
  const raw = window.localStorage.getItem(STORAGE_KEY);
  return raw === "en" || raw === "es" || raw === "gl" ? raw : DEFAULT_LANG;
}

export function I18nProvider({ children }: { children: React.ReactNode }) {
  // Render server-side and first client paint with the default language to avoid
  // hydration mismatch; switch to the stored language on the next tick.
  const [lang, setLangState] = useState<Lang>(DEFAULT_LANG);

  useEffect(() => {
    const stored = readStoredLang();
    if (stored !== lang) setLangState(stored);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (typeof document !== "undefined") {
      document.documentElement.setAttribute("lang", lang);
    }
  }, [lang]);

  const setLang = useCallback((next: Lang) => {
    setLangState(next);
    if (typeof window !== "undefined") {
      window.localStorage.setItem(STORAGE_KEY, next);
    }
  }, []);

  const t = useCallback<I18nContextValue["t"]>(
    (key, vars) => interpolate(lookup(DICTS[lang], key), vars),
    [lang]
  );

  const value = useMemo<I18nContextValue>(() => ({ lang, setLang, t }), [lang, setLang, t]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n(): I18nContextValue {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used within <I18nProvider>");
  return ctx;
}

export function useT() {
  return useI18n().t;
}
