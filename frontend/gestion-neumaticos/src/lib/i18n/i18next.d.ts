import "i18next"

import type { DEFAULT_NAMESPACE } from "@/lib/i18n/config"
import type audit from "@/lib/i18n/locales/es/audit.json"
import type auth from "@/lib/i18n/locales/es/auth.json"
import type common from "@/lib/i18n/locales/es/common.json"
import type dev from "@/lib/i18n/locales/es/dev.json"
import type settings from "@/lib/i18n/locales/es/settings.json"

declare module "i18next" {
  interface CustomTypeOptions {
    defaultNS: typeof DEFAULT_NAMESPACE
    resources: {
      common: typeof common
      auth: typeof auth
      settings: typeof settings
      audit: typeof audit
      dev: typeof dev
    }
  }
}
