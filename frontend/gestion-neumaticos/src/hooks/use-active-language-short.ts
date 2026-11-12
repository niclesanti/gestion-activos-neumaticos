import { useTranslation } from "react-i18next"

import { SUPPORTED_LANGUAGES } from "@/lib/i18n/config"

export function useActiveLanguageShort() {
  const { i18n } = useTranslation()
  return SUPPORTED_LANGUAGES.find(
    (language) => language.value === i18n.resolvedLanguage
  )?.short
}
