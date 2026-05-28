"use client"

import { setStoredUiLanguage, t, useUiLanguage, type UiLanguage } from "@/lib/i18n"

interface LanguageSwitcherProps {
  className?: string
}

export function LanguageSwitcher({ className }: LanguageSwitcherProps) {
  const uiLanguage = useUiLanguage()

  return (
    <select
      value={uiLanguage}
      onChange={(e) => setStoredUiLanguage(e.target.value as UiLanguage)}
      className={className}
      aria-label={t("language")}
    >
      <option value="en">{t("english")}</option>
      <option value="hyw">{t("westernArmenian")}</option>
    </select>
  )
}
