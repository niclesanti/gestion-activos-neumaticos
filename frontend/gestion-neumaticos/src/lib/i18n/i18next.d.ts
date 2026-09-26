import "i18next"

import type { DEFAULT_NAMESPACE } from "@/lib/i18n/config"
import type auth from "@/lib/i18n/locales/es/auth.json"
import type common from "@/lib/i18n/locales/es/common.json"

declare module "i18next" {
  interface CustomTypeOptions {
    defaultNS: typeof DEFAULT_NAMESPACE
    resources: {
      common: typeof common
      auth: typeof auth
    }
  }
}
