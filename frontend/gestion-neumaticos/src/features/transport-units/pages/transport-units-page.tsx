import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"
import { CreateTransportUnitDialog } from "@/features/transport-units/components/create-transport-unit-dialog"
import { canPerform, useAccessLevel } from "@/lib/access"

export function TransportUnitsPage() {
  const { t } = useTranslation("transport-units")
  const accessLevel = useAccessLevel()

  return (
    <div data-slot="transport-units-page" className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        // Hoy solo administrador y editor llegan a esta ruta (ROUTE_ACCESS);
        // el chequeo deja el botón oculto si el lector gana acceso de lectura.
        actions={
          canPerform(accessLevel, "transportUnits.create") ? (
            <CreateTransportUnitDialog />
          ) : null
        }
      />
      {/* TODO: listado de unidades de transporte. */}
      <p className="text-sm text-muted-foreground">{t("empty")}</p>
    </div>
  )
}
