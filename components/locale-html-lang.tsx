"use client"

import { useEffect } from "react"
import { useUiLanguage } from "@/lib/i18n"

export function LocaleHtmlLang() {
  const uiLanguage = useUiLanguage()

  useEffect(() => {
    document.documentElement.lang = uiLanguage === "hyw" ? "hy" : "en"
  }, [uiLanguage])

  return null
}
