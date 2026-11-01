import { useTranslation } from "react-i18next"

import {
  formatAbsolute,
  formatRelative,
} from "@/features/audit/lib/format-date"

/** Fecha relativa ("hace 3 días") con la fecha completa al pasar el mouse. */
export function AuditDate({ iso }: { iso: string }) {
  const { i18n } = useTranslation("audit")
  const locale = i18n.resolvedLanguage

  return (
    <time dateTime={iso} title={formatAbsolute(iso, locale)}>
      {formatRelative(iso, locale)}
    </time>
  )
}
