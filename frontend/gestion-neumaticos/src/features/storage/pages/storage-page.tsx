import { WarehouseIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { EmptyState } from "@/components/common/empty-state"
import { PageHeader } from "@/components/common/page-header"

export function StoragePage() {
  const { t } = useTranslation("storage")

  // TODO: gestión del stock por centro de almacenamiento. Al implementarla,
  // quitar el placeholder (<EmptyState>) y `empty` en i18n.
  return (
    <div data-slot="storage-page" className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("subtitle")} />
      <EmptyState
        icon={WarehouseIcon}
        title={t("empty.title")}
        description={t("empty.description")}
      />
    </div>
  )
}
