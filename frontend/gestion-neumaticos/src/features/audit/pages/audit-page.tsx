import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"
import { ScrollableTabsList } from "@/components/common/scrollable-tabs-list"
import { Tabs, TabsContent } from "@/components/ui/tabs"

/** Entidades sobre las que se registra trazabilidad, una pestaña por entidad. */
const AUDITED_ENTITY_TABS = [
  { value: "users", labelKey: "entities.users" },
  { value: "transport-units", labelKey: "entities.transportUnits" },
  { value: "repair-services", labelKey: "entities.repairServices" },
  { value: "suppliers", labelKey: "entities.suppliers" },
  { value: "tires", labelKey: "entities.tires" },
  { value: "drivers", labelKey: "entities.drivers" },
  { value: "storage-centers", labelKey: "entities.storageCenters" },
] as const

export function AuditPage() {
  const { t } = useTranslation("audit")

  return (
    <div data-slot="audit-page" className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("subtitle")} />

      <Tabs defaultValue={AUDITED_ENTITY_TABS[0].value}>
        <ScrollableTabsList
          tabs={AUDITED_ENTITY_TABS.map((tab) => ({
            value: tab.value,
            label: t(tab.labelKey),
          }))}
        />

        {AUDITED_ENTITY_TABS.map((tab) => (
          // TODO: reemplazar el placeholder por el historial de cada entidad.
          <TabsContent key={tab.value} value={tab.value}>
            <p className="py-6 text-sm text-muted-foreground">{t("empty")}</p>
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}
