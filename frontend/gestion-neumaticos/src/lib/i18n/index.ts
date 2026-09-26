import i18n from "i18next"
import LanguageDetector from "i18next-browser-languagedetector"
import { initReactI18next } from "react-i18next"

import {
  DEFAULT_NAMESPACE,
  FALLBACK_LANGUAGE,
  LANGUAGE_STORAGE_KEY,
  NAMESPACES,
  SUPPORTED_LANGUAGES,
} from "@/lib/i18n/config"
import enAuth from "@/lib/i18n/locales/en/auth.json"
import enCommon from "@/lib/i18n/locales/en/common.json"
import esAuth from "@/lib/i18n/locales/es/auth.json"
import esCommon from "@/lib/i18n/locales/es/common.json"

export const resources = {
  es: { common: esCommon, auth: esAuth },
  en: { common: enCommon, auth: enAuth },
} as const

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: FALLBACK_LANGUAGE,
    supportedLngs: SUPPORTED_LANGUAGES.map((language) => language.value),
    load: "languageOnly",
    ns: NAMESPACES,
    defaultNS: DEFAULT_NAMESPACE,
    interpolation: {
      // React ya escapa los valores interpolados.
      escapeValue: false,
    },
    detection: {
      order: ["localStorage", "navigator", "htmlTag"],
      lookupLocalStorage: LANGUAGE_STORAGE_KEY,
      caches: ["localStorage"],
    },
    react: {
      // Los recursos viajan en el bundle: no hace falta Suspense.
      useSuspense: false,
    },
  })

export default i18n
