import { useTranslation } from "react-i18next"

import { ScrollArea } from "@/components/ui/scroll-area"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"

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
      <header className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-semibold">{t("title")}</h1>
        <p className="text-sm text-muted-foreground">{t("subtitle")}</p>
      </header>

      <Tabs defaultValue={AUDITED_ENTITY_TABS[0].value}>
        {/* Siete etiquetas largas no entran en una línea. El ScrollArea aporta
            una barra propia en vez de la genérica del navegador; el pb-3 le
            reserva lugar debajo de la lista para que no tape las pestañas. */}
        <ScrollArea orientation="horizontal" className="w-full pb-3">
          <TabsList className="w-max justify-start">
            {AUDITED_ENTITY_TABS.map((tab) => (
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

export default AuditPage
