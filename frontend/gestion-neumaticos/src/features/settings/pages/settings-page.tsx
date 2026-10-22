import type * as React from "react"

import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"
import { ScrollableTabsList } from "@/components/common/scrollable-tabs-list"
import { Tabs, TabsContent } from "@/components/ui/tabs"
import {
  canAccessSettingsTab,
  useAccessLevel,
  type SettingsTab,
} from "@/lib/access"

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
] as const satisfies readonly { value: SettingsTab; labelKey: string }[]

type SettingsPageProps = {
  /**
   * Contenido de cada catálogo. Lo compone el router, porque cada ABM es su
   * propio feature y este no puede importarlos.
   */
  catalogs?: Partial<Record<SettingsTab, React.ReactNode>>
}

export function SettingsPage({ catalogs = {} }: SettingsPageProps) {
  const { t } = useTranslation("settings")
  const accessLevel = useAccessLevel()
  // Cada nivel ve solo sus pestañas (SETTINGS_TAB_ACCESS).
  const tabs = CATALOG_TABS.filter((tab) =>
    canAccessSettingsTab(accessLevel, tab.value)
  )

  return (
    <div data-slot="settings-page" className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("subtitle")} />

      {tabs.length > 0 ? (
        <Tabs defaultValue={tabs[0].value}>
          <ScrollableTabsList
            tabs={tabs.map((tab) => ({
              value: tab.value,
              label: t(tab.labelKey),
            }))}
          />

          {tabs.map((tab) => (
            <TabsContent key={tab.value} value={tab.value}>
              {/* TODO: reemplazar el placeholder por el ABM de cada catálogo. */}
              {catalogs[tab.value] ?? (
                <p className="py-6 text-sm text-muted-foreground">
                  {t("empty")}
                </p>
              )}
            </TabsContent>
          ))}
        </Tabs>
      ) : null}
    </div>
  )
}
