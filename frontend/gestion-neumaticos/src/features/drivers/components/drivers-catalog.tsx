import { useTranslation } from "react-i18next"

import { CreateDriverDialog } from "@/features/drivers/components/create-driver-dialog"
import { canPerform, useAccessLevel } from "@/lib/access"

/** Pestaña Conductores de Configuración: la ven todos, solo editor y administrador dan de alta. */
export function DriversCatalog() {
  const { t } = useTranslation("drivers")
  const accessLevel = useAccessLevel()

  return (
    <div data-slot="drivers-catalog" className="flex flex-col gap-4 py-4">
      {/* Es solo UX: el backend responde 403 al lector igual. */}
      {canPerform(accessLevel, "drivers.create") ? (
        <div className="flex justify-end">
          <CreateDriverDialog />
        </div>
      ) : null}
      {/* TODO: listado de conductores. */}
      <p className="text-sm text-muted-foreground">{t("empty")}</p>
    </div>
  )
}
