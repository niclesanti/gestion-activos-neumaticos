import { ChevronDownIcon, TruckIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import type { TransportUnitDetail } from "@/features/transport-units/api/transport-units-api"
import type {
  SlotState,
  WheelRenderProps,
} from "@/features/transport-units/components/axle-diagram"
import { AxleDiagram } from "@/features/transport-units/components/axle-diagram"
import { SlotPopover } from "@/features/transport-units/components/slot-popover"
import { useTransportUnit } from "@/features/transport-units/hooks/use-transport-unit"
import { getHttpStatus } from "@/lib/api/client"

/**
 * Ficha de una unidad de transporte (HU-10): datos generales y el chasis con
 * el neumático montado en cada posición. Controlado por el `publicId`
 * seleccionado en el listado; sin él está cerrado y no consulta.
 */
export function TransportUnitDetailDialog({
  publicId,
  plate,
  onClose,
}: {
  publicId: string | null
  /** Patente de la fila elegida: título mientras llega la ficha. */
  plate: string
  onClose: () => void
}) {
  const { t } = useTranslation("transport-units")
  const { data, isPending, isError, error, refetch } =
    useTransportUnit(publicId)

  return (
    <Dialog
      open={publicId !== null}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
    >
      <DialogContent className="flex max-h-[calc(100dvh-4rem)] flex-col gap-0 p-0 sm:max-w-xl">
        <DialogHeader className="px-6 pt-6 pb-4">
          <div className="flex flex-wrap items-center gap-2">
            <DialogTitle>{data?.patente ?? plate}</DialogTitle>
            {data ? (
              <Badge variant="outline">
                {t(`unitTypes.${data.configuracion.tipoUnidad}`)}
              </Badge>
            ) : null}
          </div>
          <DialogDescription>{t("detail.description")}</DialogDescription>
        </DialogHeader>
        <ScrollArea className="flex min-h-0 flex-1 flex-col *:data-[slot=scroll-area-viewport]:min-h-0 *:data-[slot=scroll-area-viewport]:flex-1">
          <div className="flex flex-col gap-4 px-6 pt-2 pb-6">
            {isPending ? (
              <DetailSkeleton />
            ) : isError ? (
              <Empty className="border">
                <EmptyHeader>
                  <EmptyMedia variant="icon">
                    <TruckIcon />
                  </EmptyMedia>
                  <EmptyTitle>
                    {getHttpStatus(error) === 404
                      ? t("detail.notFound.title")
                      : t("detail.error.title")}
                  </EmptyTitle>
                  <EmptyDescription>
                    {getHttpStatus(error) === 404
                      ? t("detail.notFound.description")
                      : t("detail.error.description")}
                  </EmptyDescription>
                </EmptyHeader>
                {getHttpStatus(error) === 404 ? null : (
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => refetch()}
                  >
                    {t("detail.error.retry")}
                  </Button>
                )}
              </Empty>
            ) : (
              <UnitDetail unit={data} />
            )}
          </div>
        </ScrollArea>
      </DialogContent>
    </Dialog>
  )
}

function DetailSkeleton() {
  return (
    <div className="flex flex-col gap-4" aria-busy="true">
      <Skeleton className="h-24 w-full rounded-3xl" />
      <Skeleton className="h-80 w-full rounded-3xl" />
    </div>
  )
}

function UnitDetail({ unit }: { unit: TransportUnitDetail }) {
  const { t } = useTranslation("transport-units")
  const { configuracion } = unit
  const occupiedCount = unit.slots.filter((slot) => slot.ocupado).length
  const slotsByCode = new Map(unit.slots.map((slot) => [slot.codigo, slot]))
  const slotStates: Record<string, SlotState> = Object.fromEntries(
    unit.slots.map((slot) => [
      slot.codigo,
      {
        occupied: slot.ocupado,
        description: slot.neumatico?.numeroSerie ?? undefined,
      },
    ])
  )

  function renderWheel({ code, className }: WheelRenderProps) {
    const slot = slotsByCode.get(code)
    return slot ? (
      <SlotPopover slot={slot} className={className} />
    ) : (
      <div className={className} />
    )
  }

  return (
    <>
      <Collapsible
        defaultOpen
        className="flex flex-col rounded-3xl border bg-muted/30"
      >
        <CollapsibleTrigger
          render={
            <Button
              type="button"
              variant="ghost"
              className="group h-auto justify-between rounded-3xl px-4 py-3"
            />
          }
        >
          {t("detail.data.title")}
          <ChevronDownIcon className="transition-transform group-data-panel-open:rotate-180" />
        </CollapsibleTrigger>
        <CollapsibleContent>
          <Separator />
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 px-4 py-3 text-sm">
            <dt className="text-muted-foreground">
              {t("detail.data.configuration")}
            </dt>
            <dd>{configuracion.nombre}</dd>
            <dt className="text-muted-foreground">{t("detail.data.type")}</dt>
            <dd>{t(`unitTypes.${configuracion.tipoUnidad}`)}</dd>
            <dt className="text-muted-foreground">{t("detail.data.axles")}</dt>
            <dd>{configuracion.totalEjes}</dd>
            <dt className="text-muted-foreground">
              {t("detail.data.occupied")}
            </dt>
            <dd>
              {t("detail.data.occupiedValue", {
                occupied: occupiedCount,
                total: unit.slots.length,
              })}
            </dd>
          </dl>
        </CollapsibleContent>
      </Collapsible>
      <AxleDiagram
        configuration={configuracion}
        slotStates={slotStates}
        renderWheel={renderWheel}
      />
    </>
  )
}
