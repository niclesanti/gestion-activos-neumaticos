import { useTranslation } from "react-i18next"

import { ScrollArea } from "@/components/ui/scroll-area"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

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
      <header className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-semibold">{t("title")}</h1>
        <p className="text-sm text-muted-foreground">{t("subtitle")}</p>
      </header>

      <Tabs defaultValue={CATALOG_TABS[0].value}>
        {/* Las etiquetas en español son largas. El ScrollArea aporta una barra
            propia en vez de la genérica del navegador; el pb-3 le reserva lugar
            debajo de la lista para que no tape las pestañas. */}
        <ScrollArea orientation="horizontal" className="w-full pb-3">
          <TabsList className="w-max justify-start">
            {CATALOG_TABS.map((tab) => (
              <TabsTrigger
                key={tab.value}
                value={tab.value}
                className="flex-none"
              >
                {t(tab.labelKey)}
              </TabsTrigger>
            ))}
          </TabsList>
        </ScrollArea>

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

export default SettingsPage
