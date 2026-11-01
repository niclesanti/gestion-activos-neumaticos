import { useTranslation } from "react-i18next"

import { Badge } from "@/components/ui/badge"
import type { AuditRecord } from "@/features/audit/api/audit-api"

export function AuditStatusBadge({ status }: { status: AuditRecord["estado"] }) {
  const { t } = useTranslation("audit")

  return (
    <Badge variant={status === "BAJA" ? "destructive" : "secondary"}>
      {t(`status.${status}`)}
    </Badge>
  )
}
