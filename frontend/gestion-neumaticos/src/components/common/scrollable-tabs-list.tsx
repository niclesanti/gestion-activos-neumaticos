import { ScrollArea } from "@/components/ui/scroll-area"
import { TabsList, TabsTrigger } from "@/components/ui/tabs"
import { cn } from "@/lib/utils"

export type ScrollableTab = {
  value: string
  label: string
}

/**
 * Lista de pestañas que no entra en una línea (etiquetas largas en español).
 * El ScrollArea aporta una barra propia en vez de la genérica del navegador; el
 * pb-3 le reserva lugar debajo de la lista para que no tape las pestañas.
 *
 * Va dentro de un <Tabs>, como cualquier TabsList.
 */
export function ScrollableTabsList({
  tabs,
  className,
  ...props
}: React.ComponentProps<typeof ScrollArea> & {
  tabs: readonly ScrollableTab[]
}) {
  return (
    <ScrollArea
      orientation="horizontal"
      className={cn("w-full pb-3", className)}
      {...props}
    >
      <TabsList className="w-max justify-start">
        {tabs.map((tab) => (
          <TabsTrigger key={tab.value} value={tab.value} className="flex-none">
            {tab.label}
          </TabsTrigger>
        ))}
      </TabsList>
    </ScrollArea>
  )
}
