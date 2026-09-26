import * as React from "react"
import { useTranslation } from "react-i18next"

import {
  LANGUAGE_STORAGE_KEY,
  SUPPORTED_LANGUAGES,
  type Language,
} from "@/lib/i18n/config"

type LanguageProviderProps = {
  children: React.ReactNode
}

function isLanguage(value: string | null): value is Language {
  if (value === null) {
    return false
  }

  return SUPPORTED_LANGUAGES.some((language) => language.value === value)
}

/**
 * i18next es la única fuente de verdad del idioma: este provider sólo sincroniza
 * los efectos que viven fuera de React (el atributo `lang` del documento y el
 * idioma elegido en otras pestañas), igual que hace ThemeProvider con el tema.
 */
export function LanguageProvider({ children }: LanguageProviderProps) {
  const { i18n } = useTranslation()
  const resolvedLanguage = i18n.resolvedLanguage

  React.useEffect(() => {
    if (resolvedLanguage) {
      document.documentElement.lang = resolvedLanguage
    }
  }, [resolvedLanguage])

  React.useEffect(() => {
    const handleStorageChange = (event: StorageEvent) => {
      if (event.storageArea !== localStorage) {
        return
      }

      if (event.key !== LANGUAGE_STORAGE_KEY) {
        return
      }

      if (isLanguage(event.newValue)) {
        void i18n.changeLanguage(event.newValue)
      }
    }

    window.addEventListener("storage", handleStorageChange)

    return () => {
      window.removeEventListener("storage", handleStorageChange)
    }
  }, [i18n])

  return children
}
