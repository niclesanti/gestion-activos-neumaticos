import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"
import { CreateTireDialog } from "@/features/tires/components/create-tire-dialog"

export function TiresPage() {
  const { t } = useTranslation("tires")

  return (
    <div data-slot="tires-page" className="flex flex-col gap-6">
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        // Sin chequeo de rol acá: a esta ruta solo llegan administrador y
        // editor (ROUTE_ACCESS), los dos pueden registrar, y el backend
        // responde 403 a cualquier otro. Si el lector gana acceso de lectura a
        // la sección, el botón tendrá que ocultarse según su nivel.
        actions={<CreateTireDialog />}
      />
      {/* TODO: listado de neumáticos. */}
    </div>
  )
}
