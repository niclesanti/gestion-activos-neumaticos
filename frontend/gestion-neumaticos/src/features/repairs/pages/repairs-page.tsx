import { WrenchIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { EmptyState } from "@/components/common/empty-state"
import { PageHeader } from "@/components/common/page-header"

export function RepairsPage() {
  const { t } = useTranslation("repairs")

  // TODO: gestión de reparaciones (envío y reingreso de neumáticos). Al
  // implementarla, quitar el placeholder (<EmptyState>) y `empty` en i18n.
  return (
    <div data-slot="repairs-page" className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("subtitle")} />
      <EmptyState
        icon={WrenchIcon}
        title={t("empty.title")}
        description={t("empty.description")}
      />
    </div>
  )
}
