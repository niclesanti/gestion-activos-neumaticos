import * as React from "react"

import { ArrowLeftRightIcon, PlusIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { Button } from "@/components/ui/button"
import {
  Popover,
  PopoverContent,
  PopoverHeader,
  PopoverTitle,
  PopoverTrigger,
} from "@/components/ui/popover"
import type { UnitSlotDetail } from "@/features/transport-units/api/transport-units-api"
import { cn } from "@/lib/utils"

/**
 * Una posición de la ficha: la rueda es el botón (teclado, táctil y lector de
 * pantalla) y abre, también al pasar el mouse, los datos del neumático
 * montado o la indicación de que está vacía. Con `onMount` (editor y
 * administrador, HU-31) suma la acción de montar o reemplazar el neumático;
 * sin ella (lector) solo muestra los datos.
 */
export function SlotPopover({
  slot,
  className,
  onMount,
}: {
  slot: UnitSlotDetail
  className: string
  onMount?: (slot: UnitSlotDetail) => void
}) {
  const { t } = useTranslation("transport-units")
  const [open, setOpen] = React.useState(false)
  const state = slot.ocupado ? t("legend.occupied") : t("legend.empty")

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        openOnHover
        delay={150}
        aria-label={`${slot.codigo}: ${state}`}
        className={cn(
          "cursor-pointer outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
          className
        )}
      />
      <PopoverContent className="w-64 gap-3">
        <PopoverHeader>
          <PopoverTitle className="font-mono">{slot.codigo}</PopoverTitle>
        </PopoverHeader>
        {slot.neumatico ? (
          <TireData tire={slot.neumatico} />
        ) : (
          <EmptySlot slot={slot} />
        )}
        {onMount ? (
          <Button
            type="button"
            size="sm"
            variant={slot.ocupado ? "outline" : "default"}
            onClick={() => {
              setOpen(false)
              onMount(slot)
            }}
          >
            {slot.ocupado ? (
              <ArrowLeftRightIcon data-icon="inline-start" />
            ) : (
              <PlusIcon data-icon="inline-start" />
            )}
            {slot.ocupado ? t("mount.replaceAction") : t("mount.mountAction")}
          </Button>
        ) : null}
      </PopoverContent>
    </Popover>
  )
}

function TireData({
  tire,
}: {
  tire: NonNullable<UnitSlotDetail["neumatico"]>
}) {
  const { t, i18n } = useTranslation("transport-units")
  const notRegistered = t("slot.notRegistered")

  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-3 gap-y-1.5">
      <dt className="text-muted-foreground">{t("slot.serial")}</dt>
      <dd className="font-mono">{tire.numeroSerie ?? notRegistered}</dd>
      <dt className="text-muted-foreground">{t("slot.brand")}</dt>
      <dd>{tire.marca}</dd>
      <dt className="text-muted-foreground">{t("slot.state")}</dt>
      <dd>{t(`tireStates.${tire.estado}`, { defaultValue: tire.estado })}</dd>
      <dt className="text-muted-foreground">{t("slot.mileage")}</dt>
      <dd>
        {tire.kilometrajeTotal === null
          ? notRegistered
          : t("slot.mileageValue", {
              value: tire.kilometrajeTotal.toLocaleString(i18n.language),
            })}
      </dd>
    </dl>
  )
}

function EmptySlot({ slot }: { slot: UnitSlotDetail }) {
  const { t } = useTranslation("transport-units")

  return (
    <div className="flex flex-col gap-1">
      <p className="text-muted-foreground">
        {slot.tipoEje
          ? t("slot.axleType", { type: t(`axleTypes.${slot.tipoEje}`) })
          : t("preview.spares")}
      </p>
      <p>{t("slot.noTire")}</p>
    </div>
  )
}
