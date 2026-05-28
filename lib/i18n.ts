import { useEffect, useState } from "react"

export type UiLanguage = "en" | "hyw"
export type LyricContentLanguage = "ENGLISH" | "WESTERN_ARMENIAN"

export const UI_LANGUAGE_STORAGE_KEY = "alik.ui-language"
export const DEFAULT_UI_LANGUAGE: UiLanguage = "en"

const en = {
  // Song creator
  createNewSong: "Create New Song",
  songTitle: "Song Title",
  lyrics: "Lyrics",
  stylePrompt: "Style Prompt",
  importFromMyLyrics: "Import from My Lyrics",
  importFromPublicLyrics: "Import from Public Lyrics",
  generateSong: "Generate Song",
  lyricsMinChars: "Lyrics must be at least 50 characters.",

  // Lyrics page
  readyLibrary: "Ready Library",
  save: "Save",
  delete: "Delete",
  edit: "Edit",
  restore: "Restore",
  restoreAsNewCopy: "Restore as New Copy",

  // Language names / UI
  english: "English",
  westernArmenian: "Western Armenian",
  lyricsLanguage: "Lyrics language",

  // Misc (used in future/other components)
  loadingTrack: "Loading track...",
} as const

const hyw = {
  createNewSong: "Ստեղծել նոր երգ",
  songTitle: "Երգի վերնագիր",
  lyrics: "Բառեր",
  stylePrompt: "Ոճի նկարագրություն",
  importFromMyLyrics: "Ներմուծել իմ բառերից",
  importFromPublicLyrics: "Ներմուծել հանրային բառերից",
  generateSong: "Ստեղծել երգ",
  lyricsMinChars: "Բառերը պետք է լինեն առնվազն 50 նիշ։",

  readyLibrary: "Պատրաստի գրադարան",
  save: "Պահպանել",
  delete: "Ջնջել",
  edit: "Խմբագրել",
  restore: "Վերականգնել",
  restoreAsNewCopy: "Վերականգնել որպես նոր օրինակ",

  english: "English",
  westernArmenian: "Արեւմտահայերէն",
  lyricsLanguage: "Բառերի լեզու",

  loadingTrack: "Բեռնում եմ երգը...",
} as const

const translations: Record<UiLanguage, Record<keyof typeof en, string>> = {
  en: en as Record<keyof typeof en, string>,
  hyw: hyw as Record<keyof typeof en, string>,
}

export type TranslationKey = keyof typeof en

export function getStoredUiLanguage(): UiLanguage {
  if (typeof window === "undefined") return DEFAULT_UI_LANGUAGE
  const raw = window.localStorage.getItem(UI_LANGUAGE_STORAGE_KEY)
  if (raw === "en" || raw === "hyw") return raw
  return DEFAULT_UI_LANGUAGE
}

export function setStoredUiLanguage(language: UiLanguage) {
  if (typeof window === "undefined") return
  window.localStorage.setItem(UI_LANGUAGE_STORAGE_KEY, language)
  // Notify other mounted components.
  window.dispatchEvent(new Event("alik-ui-language-changed"))
}

export function t(key: TranslationKey, language?: UiLanguage): string {
  const lang = language ?? getStoredUiLanguage()
  return translations[lang][key]
}

export function useUiLanguage(): UiLanguage {
  const [language, setLanguage] = useState<UiLanguage>(() => getStoredUiLanguage())

  useEffect(() => {
    const onChange = () => setLanguage(getStoredUiLanguage())
    window.addEventListener("alik-ui-language-changed", onChange)
    return () => window.removeEventListener("alik-ui-language-changed", onChange)
  }, [])

  return language
}

export function parseLyricContentLanguage(value: string | null | undefined): LyricContentLanguage {
  const normalized = value?.trim()
  if (normalized === "ENGLISH" || normalized === "WESTERN_ARMENIAN") return normalized
  return "WESTERN_ARMENIAN"
}

