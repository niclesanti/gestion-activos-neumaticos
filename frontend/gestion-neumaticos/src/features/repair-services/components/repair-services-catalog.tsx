import { useTranslation } from "react-i18next"

import { CreateRepairServiceDialog } from "@/features/repair-services/components/create-repair-service-dialog"
import { canPerform, useAccessLevel } from "@/lib/access"

/** Pestaña Servicios de reparación de Configuración: la ven todos, solo editor y administrador dan de alta. */
export function RepairServicesCatalog() {
  const { t } = useTranslation("repair-services")
  const accessLevel = useAccessLevel()

  return (
    <div
      data-slot="repair-services-catalog"
      className="flex flex-col gap-4 py-4"
    >
      <header className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex flex-col gap-1">
          <h2 className="font-heading text-lg font-semibold">{t("title")}</h2>
          <p className="text-sm text-muted-foreground">{t("description")}</p>
        </div>
        {/* Es solo UX: el backend responde 403 al lector igual. */}
        {canPerform(accessLevel, "repairServices.create") ? (
          <div className="flex shrink-0 items-center gap-2">
            <CreateRepairServiceDialog />
          </div>
        ) : null}
      </header>
      {/* TODO: listado de servicios de reparación. */}
      <p className="text-sm text-muted-foreground">{t("empty")}</p>
    </div>
  )
}
