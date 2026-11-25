import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"
import { CreateTireDialog } from "@/features/tires/components/create-tire-dialog"
import { TiresTable } from "@/features/tires/components/tires-table"
import { canPerform, useAccessLevel } from "@/lib/access"

export function TiresPage() {
  const { t } = useTranslation("tires")
  const accessLevel = useAccessLevel()

  return (
    <div data-slot="tires-page" className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        // El lector también llega a esta ruta (solo lectura): el botón de alta
        // queda oculto para él.
        actions={
          canPerform(accessLevel, "tires.create") ? <CreateTireDialog /> : null
        }
      />
      <TiresTable />
    </div>
  )
}
