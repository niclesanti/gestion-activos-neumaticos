import { useTranslation } from "react-i18next"

import type { Auditor } from "@/features/audit/api/audit-api"

/**
 * Quién firmó una operación: nombre y @usuario. Sin firma es una carga previa a
 * la auditoría o un proceso del sistema; con firma pero sin datos, el usuario ya
 * no existe.
 */
export function AuditorLabel({ auditor }: { auditor: Auditor | null }) {
  const { t } = useTranslation("audit")

  if (!auditor) {
    return <span className="text-muted-foreground">{t("system")}</span>
  }
  if (!auditor.nombreApellido && !auditor.nombreUsuario) {
    return <span className="text-muted-foreground">{t("unknownUser")}</span>
  }

  return (
    <span className="flex min-w-0 flex-col">
      <span className="truncate">
        {auditor.nombreApellido ?? auditor.nombreUsuario}
      </span>
      {auditor.nombreUsuario && auditor.nombreApellido ? (
        <span className="truncate text-xs text-muted-foreground">
          @{auditor.nombreUsuario}
        </span>
      ) : null}
    </span>
  )
}
