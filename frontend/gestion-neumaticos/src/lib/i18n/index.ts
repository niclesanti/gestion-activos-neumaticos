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
import enAddress from "@/lib/i18n/locales/en/address.json"
import enAudit from "@/lib/i18n/locales/en/audit.json"
import enAuth from "@/lib/i18n/locales/en/auth.json"
import enCommon from "@/lib/i18n/locales/en/common.json"
import enDev from "@/lib/i18n/locales/en/dev.json"
import enDrivers from "@/lib/i18n/locales/en/drivers.json"
import enHome from "@/lib/i18n/locales/en/home.json"
import enRepairServices from "@/lib/i18n/locales/en/repair-services.json"
import enSettings from "@/lib/i18n/locales/en/settings.json"
import enSuppliers from "@/lib/i18n/locales/en/suppliers.json"
import enTires from "@/lib/i18n/locales/en/tires.json"
import enTransportUnits from "@/lib/i18n/locales/en/transport-units.json"
import esAddress from "@/lib/i18n/locales/es/address.json"
import esAudit from "@/lib/i18n/locales/es/audit.json"
import esAuth from "@/lib/i18n/locales/es/auth.json"
import esCommon from "@/lib/i18n/locales/es/common.json"
import esDev from "@/lib/i18n/locales/es/dev.json"
import esDrivers from "@/lib/i18n/locales/es/drivers.json"
import esHome from "@/lib/i18n/locales/es/home.json"
import esRepairServices from "@/lib/i18n/locales/es/repair-services.json"
import esSettings from "@/lib/i18n/locales/es/settings.json"
import esSuppliers from "@/lib/i18n/locales/es/suppliers.json"
import esTires from "@/lib/i18n/locales/es/tires.json"
import esTransportUnits from "@/lib/i18n/locales/es/transport-units.json"

export const resources = {
  es: {
    common: esCommon,
    auth: esAuth,
    settings: esSettings,
    audit: esAudit,
    dev: esDev,
    home: esHome,
    tires: esTires,
    drivers: esDrivers,
    suppliers: esSuppliers,
    "repair-services": esRepairServices,
    address: esAddress,
    "transport-units": esTransportUnits,
  },
  en: {
    common: enCommon,
    auth: enAuth,
    settings: enSettings,
    audit: enAudit,
    dev: enDev,
    home: enHome,
    tires: enTires,
    drivers: enDrivers,
    suppliers: enSuppliers,
    "repair-services": enRepairServices,
    address: enAddress,
    "transport-units": enTransportUnits,
  },
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
