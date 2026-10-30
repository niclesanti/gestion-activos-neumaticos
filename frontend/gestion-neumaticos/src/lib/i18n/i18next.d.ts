import "i18next"

import type { DEFAULT_NAMESPACE } from "@/lib/i18n/config"
import type audit from "@/lib/i18n/locales/es/audit.json"
import type auth from "@/lib/i18n/locales/es/auth.json"
import type common from "@/lib/i18n/locales/es/common.json"
import type dev from "@/lib/i18n/locales/es/dev.json"
import type drivers from "@/lib/i18n/locales/es/drivers.json"
import type home from "@/lib/i18n/locales/es/home.json"
import type settings from "@/lib/i18n/locales/es/settings.json"
import type suppliers from "@/lib/i18n/locales/es/suppliers.json"
import type tires from "@/lib/i18n/locales/es/tires.json"
import type transportUnits from "@/lib/i18n/locales/es/transport-units.json"

declare module "i18next" {
  interface CustomTypeOptions {
    defaultNS: typeof DEFAULT_NAMESPACE
    resources: {
      common: typeof common
      auth: typeof auth
      settings: typeof settings
      audit: typeof audit
      dev: typeof dev
      home: typeof home
      tires: typeof tires
      drivers: typeof drivers
      suppliers: typeof suppliers
      "transport-units": typeof transportUnits
    }
  }
}
