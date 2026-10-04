import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"
import { ScrollableTabsList } from "@/components/common/scrollable-tabs-list"
import { Tabs, TabsContent } from "@/components/ui/tabs"

/**
 * ABM de las entidades de uso menos frecuente. Viven acá, y no en la sidebar,
 * para no competir con las operaciones del día a día.
 */
const CATALOG_TABS = [
  { value: "users", labelKey: "catalogs.users" },
  { value: "drivers", labelKey: "catalogs.drivers" },
  { value: "suppliers", labelKey: "catalogs.suppliers" },
  { value: "repair-services", labelKey: "catalogs.repairServices" },
  { value: "storage-centers", labelKey: "catalogs.storageCenters" },
] as const

export function SettingsPage() {
  const { t } = useTranslation("settings")

  return (
    <div data-slot="settings-page" className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("subtitle")} />

      <Tabs defaultValue={CATALOG_TABS[0].value}>
        <ScrollableTabsList
          tabs={CATALOG_TABS.map((tab) => ({
            value: tab.value,
            label: t(tab.labelKey),
          }))}
        />

        {CATALOG_TABS.map((tab) => (
          // TODO: reemplazar el placeholder por el ABM de cada catálogo.
          <TabsContent key={tab.value} value={tab.value}>
            <p className="py-6 text-sm text-muted-foreground">{t("empty")}</p>
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}
