import * as React from "react"

import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"
import { ScrollableTabsList } from "@/components/common/scrollable-tabs-list"
import { Tabs, TabsContent } from "@/components/ui/tabs"
import {
  AUDITED_ENTITIES,
  type AuditedEntity,
} from "@/features/audit/api/audit-api"
import { AuditRecordsTable } from "@/features/audit/components/audit-records-table"

/** Clave i18n del nombre de cada entidad auditada (una pestaña por entidad). */
const ENTITY_LABEL_KEYS = {
  users: "entities.users",
  "transport-units": "entities.transportUnits",
  "repair-services": "entities.repairServices",
  suppliers: "entities.suppliers",
  tires: "entities.tires",
  drivers: "entities.drivers",
  "storage-centers": "entities.storageCenters",
} as const satisfies Record<AuditedEntity, string>

export function AuditPage() {
  const { t } = useTranslation("audit")
  const [activeEntity, setActiveEntity] = React.useState<AuditedEntity>(
    AUDITED_ENTITIES[0]
  )

  return (
    <div data-slot="audit-page" className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("subtitle")} />

      <Tabs
        value={activeEntity}
        onValueChange={(value) => setActiveEntity(value as AuditedEntity)}
      >
        <ScrollableTabsList
          tabs={AUDITED_ENTITIES.map((entity) => ({
            value: entity,
            label: t(ENTITY_LABEL_KEYS[entity]),
          }))}
        />

        {AUDITED_ENTITIES.map((entity) => (
          <TabsContent key={entity} value={entity}>
            <AuditRecordsTable
              entity={entity}
              active={entity === activeEntity}
            />
          </TabsContent>
        ))}
      </Tabs>
    </div>
  )
}
