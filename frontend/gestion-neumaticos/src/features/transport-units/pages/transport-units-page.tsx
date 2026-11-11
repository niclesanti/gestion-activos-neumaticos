import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"
import { CreateTransportUnitDialog } from "@/features/transport-units/components/create-transport-unit-dialog"
import { TransportUnitsTable } from "@/features/transport-units/components/transport-units-table"
import { canPerform, useAccessLevel } from "@/lib/access"

export function TransportUnitsPage() {
  const { t } = useTranslation("transport-units")
  const accessLevel = useAccessLevel()

  return (
    <div data-slot="transport-units-page" className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        // El lector también llega a esta ruta (solo lectura): el botón de alta
        // queda oculto para él.
        actions={
          canPerform(accessLevel, "transportUnits.create") ? (
            <CreateTransportUnitDialog />
          ) : null
        }
      />
      <TransportUnitsTable />
    </div>
  )
}
