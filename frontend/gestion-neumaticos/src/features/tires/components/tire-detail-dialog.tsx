import { ChevronDownIcon, CircleDotIcon } from "lucide-react"
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
  DialogFooter,
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
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip"
import type { TireDetail } from "@/features/tires/api/tires-api"
import { useTire } from "@/features/tires/hooks/use-tire"
import { getHttpStatus } from "@/lib/api/client"

/**
 * Ficha de un neumático (HU-14): datos técnicos, situación operativa vigente y
 * dónde está. Controlado por el `publicId` seleccionado en el listado; sin él
 * está cerrado y no consulta.
 */
export function TireDetailDialog({
  publicId,
  serialNumber,
  onClose,
}: {
  publicId: string | null
  /** Número de serie de la fila elegida: título mientras llega la ficha. */
  serialNumber: string | null
  onClose: () => void
}) {
  const { t } = useTranslation("tires")
  const { data, isPending, isError, error, refetch } = useTire(publicId)
  const title = data ? data.numeroSerie : serialNumber

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
            <DialogTitle>{title ?? t("list.noSerialNumber")}</DialogTitle>
            {data ? (
              <>
                <Badge variant="outline">
                  {t(`status.${data.estadoActual}`)}
                </Badge>
                <Badge variant="secondary">
                  {t(`situation.${data.situacionOperativaVigente}`)}
                </Badge>
              </>
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
                    <CircleDotIcon />
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
              <TireDetailContent tire={data} />
            )}
          </div>
        </ScrollArea>
        <DialogFooter className="border-t px-6 py-4">
          <ComingSoonButton label={t("detail.actions.history")} />
          <ComingSoonButton label={t("detail.actions.location")} />
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

/**
 * Acción que todavía no existe (historial: HU-33, ubicación: HU-34). El botón
 * deshabilitado no recibe hover ni foco, por eso el tooltip cuelga de un
 * contenedor enfocable.
 */
function ComingSoonButton({ label }: { label: string }) {
  const { t } = useTranslation("tires")

  return (
    <Tooltip>
      <TooltipTrigger
        render={<span tabIndex={0} className="inline-flex" />}
        className="rounded-3xl outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <Button type="button" variant="outline" disabled aria-disabled="true">
          {label}
        </Button>
      </TooltipTrigger>
      <TooltipContent>{t("detail.actions.comingSoon")}</TooltipContent>
    </Tooltip>
  )
}

function DetailSkeleton() {
  return (
    <div className="flex flex-col gap-4" aria-busy="true">
      <Skeleton className="h-56 w-full rounded-3xl" />
      <Skeleton className="h-24 w-full rounded-3xl" />
    </div>
  )
}

function TireDetailContent({ tire }: { tire: TireDetail }) {
  const { t, i18n } = useTranslation("tires")
  const notRegistered = (
    <span className="text-muted-foreground">{t("detail.notRegistered")}</span>
  )

  function formatMileage(value: number | null) {
    return value === null
      ? notRegistered
      : t("list.mileageValue", { value: value.toLocaleString(i18n.language) })
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
              {t("detail.data.serialNumber")}
            </dt>
            <dd>{tire.numeroSerie ?? notRegistered}</dd>
            <dt className="text-muted-foreground">{t("detail.data.brand")}</dt>
            <dd>{tire.marca}</dd>
            <dt className="text-muted-foreground">
              {t("detail.data.mileage")}
            </dt>
            <dd>{formatMileage(tire.kilometrajeTotal)}</dd>
            <dt className="text-muted-foreground">
              {t("detail.data.mileageSinceRepair")}
            </dt>
            <dd>{formatMileage(tire.kilometrajeUltimaReparacion)}</dd>
            <dt className="text-muted-foreground">
              {t("detail.data.originAxle")}
            </dt>
            <dd>{t(`axle.${tire.tipoOrigen}`)}</dd>
            <dt className="text-muted-foreground">
              {t("detail.data.currentAxle")}
            </dt>
            <dd>{t(`axle.${tire.tipoActual}`)}</dd>
            <dt className="text-muted-foreground">
              {t("detail.data.retreads")}
            </dt>
            <dd>{tire.cantidadRecapados ?? notRegistered}</dd>
            <dt className="text-muted-foreground">
              {t("detail.data.comment")}
            </dt>
            <dd className="break-words whitespace-pre-line">
              {tire.comentario ?? notRegistered}
            </dd>
          </dl>
        </CollapsibleContent>
      </Collapsible>

      <section className="flex flex-col gap-2 rounded-3xl border bg-muted/30 px-4 py-3">
        <h3 className="text-sm font-medium">{t("detail.location.title")}</h3>
        <TireLocation tire={tire} />
      </section>
    </>
  )
}

/** Dónde está el neumático según su situación; nunca se inventa un dato. */
function TireLocation({ tire }: { tire: TireDetail }) {
  const { t } = useTranslation("tires")
  const { ubicacion, situacionOperativaVigente: situation } = tire

  if (tire.estadoActual === "DE_BAJA" || situation === "DE_BAJA") {
    return <p className="text-sm">{t("detail.location.deregistered")}</p>
  }
  if (situation === "EN_ESPERA") {
    return <p className="text-sm">{t("detail.location.waiting")}</p>
  }

  if (situation === "MONTADO" && ubicacion.unidad && ubicacion.posicion) {
    return (
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-sm">
        <dt className="text-muted-foreground">{t("detail.location.unit")}</dt>
        <dd>{ubicacion.unidad}</dd>
        <dt className="text-muted-foreground">
          {t("detail.location.position")}
        </dt>
        <dd>{ubicacion.posicion}</dd>
      </dl>
    )
  }
  if (situation === "EN_STOCK" && ubicacion.centroAlmacenamiento) {
    return (
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-sm">
        <dt className="text-muted-foreground">
          {t("detail.location.storageCenter")}
        </dt>
        <dd>{ubicacion.centroAlmacenamiento}</dd>
      </dl>
    )
  }
  if (situation === "EN_REPARACION" && ubicacion.servicioReparacion) {
    return (
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1.5 text-sm">
        <dt className="text-muted-foreground">
          {t("detail.location.repairService")}
        </dt>
        <dd>{ubicacion.servicioReparacion}</dd>
      </dl>
    )
  }
  return (
    <p className="text-sm text-muted-foreground">
      {t("detail.location.unavailable")}
    </p>
  )
}
