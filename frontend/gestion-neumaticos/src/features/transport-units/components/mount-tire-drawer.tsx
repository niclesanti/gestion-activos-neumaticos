import * as React from "react"

import {
  ArrowLeftIcon,
  CheckIcon,
  InfoIcon,
  Loader2Icon,
  TriangleAlertIcon,
} from "lucide-react"
import { useTranslation } from "react-i18next"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox"
import {
  Drawer,
  DrawerContent,
  DrawerDescription,
  DrawerFooter,
  DrawerHeader,
  DrawerTitle,
} from "@/components/ui/drawer"
import { Separator } from "@/components/ui/separator"
import type { DriverOption } from "@/features/transport-units/api/drivers-api"
import type {
  TransportUnitDetail,
  UnitSlotDetail,
} from "@/features/transport-units/api/transport-units-api"
import { useAssignTire } from "@/features/transport-units/hooks/use-assign-tire"
import { useDriverSearch } from "@/features/transport-units/hooks/use-driver-search"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { useIsMobile } from "@/hooks/use-mobile"
import { getHttpStatus } from "@/lib/api/client"
import {
  useTirePicker,
  type PickedTire,
  type RenderTirePicker,
  type TirePickerProps,
} from "@/lib/tire-picker"
import { notify } from "@/lib/toast"
import { cn } from "@/lib/utils"

const STEPS = ["tire", "driver", "confirm"] as const
type Step = (typeof STEPS)[number]

type Notice = { tone: "warning" | "error"; message: string }

function driverLabel(driver: DriverOption) {
  return `${driver.apellido}, ${driver.nombre}`
}

/**
 * Montaje guiado de un neumático en una posición (HU-31), en tres pasos:
 * neumático, conductor (opcional) y confirmación. Es un panel no modal (a la
 * derecha en escritorio, abajo en el celular) para que el chasis de la ficha
 * siga a la vista con la posición destino resaltada. Si la posición está
 * ocupada, el mismo flujo es el reemplazo con la advertencia de CA3.
 */
export function MountTireDrawer({
  unit,
  slotCode,
  open,
  onOpenChange,
  onRefreshUnit,
}: {
  unit: TransportUnitDetail
  /** Posición destino; se conserva durante la animación de cierre. */
  slotCode: string | null
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Vuelve a leer la ficha (tras un 409) y devuelve la versión nueva. */
  onRefreshUnit: () => Promise<TransportUnitDetail | undefined>
}) {
  const isMobile = useIsMobile()
  const slot = unit.slots.find((candidate) => candidate.codigo === slotCode)

  return (
    <Drawer
      open={open && slot !== undefined}
      onOpenChange={onOpenChange}
      // No modal: la ficha queda visible y operable al lado del panel.
      modal={false}
      // Un clic en el chasis no descarta lo elegido: se cierra con Cancelar o Esc.
      disablePointerDismissal
      swipeDirection={isMobile ? "down" : "right"}
    >
      <DrawerContent
        initialFocus={() => document.getElementById("tire-picker-search")}
        className="data-[swipe-axis=y]:[--drawer-content-max-height:75dvh] data-[swipe-axis=x]:sm:[--drawer-content-width:28rem]"
      >
        {slot ? (
          <MountFlow
            // Cada apertura arranca de cero en el paso 1.
            key={`${slot.codigo}-${String(open)}`}
            unit={unit}
            slot={slot}
            onDone={() => onOpenChange(false)}
            onRefreshUnit={onRefreshUnit}
          />
        ) : null}
      </DrawerContent>
    </Drawer>
  )
}

function MountFlow({
  unit,
  slot,
  onDone,
  onRefreshUnit,
}: {
  unit: TransportUnitDetail
  slot: UnitSlotDetail
  onDone: () => void
  onRefreshUnit: () => Promise<TransportUnitDetail | undefined>
}) {
  const { t } = useTranslation("transport-units")
  const renderTirePicker = useTirePicker()
  const assign = useAssignTire(unit.publicId)
  const [step, setStep] = React.useState<Step>("tire")
  const [tire, setTire] = React.useState<PickedTire | null>(null)
  const [driver, setDriver] = React.useState<DriverOption | null>(null)
  // Lo aceptado vale para el neumático saliente que se mostró: si la ficha
  // cambia mientras tanto, hay que volver a aceptar.
  const [acknowledgedFor, setAcknowledgedFor] = React.useState<string | null>(
    null
  )
  const [notice, setNotice] = React.useState<Notice | null>(null)
  const headingRef = React.useRef<HTMLHeadingElement>(null)

  const outgoing = slot.neumatico
  const replacing = outgoing !== null
  const ackKey = outgoing?.publicId ?? "empty"
  const acknowledged = acknowledgedFor === ackKey
  const stepIndex = STEPS.indexOf(step)
  const slotKind = slot.tipoEje
    ? t(`axleTypes.${slot.tipoEje}`)
    : t("mount.spare")

  function goTo(next: Step) {
    setStep(next)
    // El foco va al título del paso para teclado y lector de pantalla; el
    // primer paso lo toma la búsqueda.
    if (next !== "tire") {
      window.requestAnimationFrame(() => headingRef.current?.focus())
    }
  }

  function chooseTire(picked: PickedTire) {
    setTire(picked)
    setNotice(null)
    goTo("driver")
  }

  async function handleConflict() {
    const fresh = await onRefreshUnit()
    const freshSlot = fresh?.slots.find((s) => s.codigo === slot.codigo)
    const takenBy = freshSlot?.neumatico?.publicId
    if (takenBy && takenBy !== outgoing?.publicId) {
      // Alguien ocupó la posición mientras tanto: ahora es un reemplazo.
      setNotice({ tone: "warning", message: t("mount.errors.slotTaken") })
      setAcknowledgedFor(null)
      goTo("confirm")
      return
    }
    setNotice({ tone: "warning", message: t("mount.errors.tireUnavailable") })
    setTire(null)
    goTo("tire")
  }

  function submit() {
    if (!tire) return
    assign.mutate(
      {
        slotCode: slot.codigo,
        body: {
          neumaticoPublicId: tire.publicId,
          conductorPublicId: driver?.publicId ?? null,
          reemplazar: replacing,
        },
      },
      {
        onSuccess: () => {
          notify.success({
            titleKey: replacing
              ? "transport-units:mount.feedback.replaced"
              : "transport-units:mount.feedback.mounted",
            descriptionKey: "transport-units:mount.feedback.description",
            values: {
              serial: tire.numeroSerie ?? t("slot.notRegistered"),
              plate: unit.patente,
              position: slot.codigo,
            },
          })
          onDone()
        },
        onError: (error) => {
          const status = getHttpStatus(error)
          if (status === 409) {
            void handleConflict()
          } else if (status === 404) {
            setNotice({ tone: "error", message: t("mount.errors.notFound") })
            setTire(null)
            void onRefreshUnit()
            goTo("tire")
          } else if (status === 403) {
            notify.error({ titleKey: "transport-units:mount.errors.forbidden" })
          } else {
            notify.error({
              titleKey: "transport-units:mount.errors.unexpected",
            })
          }
        },
      }
    )
  }

  return (
    <>
      <DrawerHeader className="gap-3 border-b pb-4 group-data-[swipe-axis=y]/drawer-popup:text-left">
        <div className="flex flex-col gap-1">
          <DrawerTitle>
            {replacing ? t("mount.titleReplace") : t("mount.title")}
          </DrawerTitle>
          <DrawerDescription className="font-mono">
            {unit.patente} · {slot.codigo} · {slotKind}
          </DrawerDescription>
        </div>
        <StepIndicator current={stepIndex} />
      </DrawerHeader>

      <div className="flex min-h-0 flex-1 flex-col gap-4 overflow-y-auto p-4">
        {notice ? (
          <Alert variant={notice.tone === "error" ? "destructive" : "default"}>
            <TriangleAlertIcon />
            <AlertDescription>{notice.message}</AlertDescription>
          </Alert>
        ) : null}

        {step === "tire" ? (
          <section className="flex flex-col gap-3">
            <StepHeading>{t("mount.steps.tire.heading")}</StepHeading>
            <p className="text-sm text-muted-foreground">
              {t("mount.steps.tire.hint")}
            </p>
            {outgoing ? (
              <Alert>
                <InfoIcon />
                <AlertTitle>{t("mount.outgoing.title")}</AlertTitle>
                <AlertDescription>
                  {t("mount.outgoing.description", {
                    serial: outgoing.numeroSerie ?? t("slot.notRegistered"),
                    brand: outgoing.marca,
                  })}
                </AlertDescription>
              </Alert>
            ) : null}
            {renderTirePicker ? (
              <TirePickerHost
                render={renderTirePicker}
                selected={tire}
                onSelect={setTire}
                onConfirm={chooseTire}
              />
            ) : null}
          </section>
        ) : step === "driver" ? (
          <section className="flex flex-col gap-3">
            <StepHeading ref={headingRef}>
              {t("mount.steps.driver.heading")}
            </StepHeading>
            <p className="text-sm text-muted-foreground">
              {t("mount.steps.driver.hint")}
            </p>
            <DriverCombobox value={driver} onChange={setDriver} />
          </section>
        ) : (
          <section className="flex flex-col gap-4">
            <StepHeading ref={headingRef}>
              {t("mount.steps.confirm.heading")}
            </StepHeading>
            <dl className="flex flex-col gap-3 rounded-3xl border bg-muted/30 p-4 text-sm">
              <SummaryRow
                label={t("mount.summary.tire")}
                onChange={() => goTo("tire")}
                changeLabel={t("mount.summary.changeTire")}
              >
                <span className="font-mono">
                  {tire?.numeroSerie ?? t("slot.notRegistered")}
                </span>
                <span className="text-muted-foreground">
                  {tire
                    ? `${tire.marca} · ${
                        tire.situacion === "EN_ESPERA"
                          ? t("mount.summary.fromWaiting")
                          : t("mount.summary.fromCenter", {
                              name: tire.centroAlmacenamiento ?? "",
                            })
                      }`
                    : null}
                </span>
              </SummaryRow>
              <Separator />
              <SummaryRow label={t("mount.summary.destination")}>
                <span className="font-mono">
                  {unit.patente} · {slot.codigo}
                </span>
                <span className="text-muted-foreground">{slotKind}</span>
              </SummaryRow>
              <Separator />
              <SummaryRow
                label={t("mount.summary.driver")}
                onChange={() => goTo("driver")}
                changeLabel={t("mount.summary.changeDriver")}
              >
                <span>
                  {driver ? driverLabel(driver) : t("mount.summary.noDriver")}
                </span>
              </SummaryRow>
              {outgoing ? (
                <>
                  <Separator />
                  <SummaryRow label={t("mount.summary.outgoing")}>
                    <span className="font-mono">
                      {outgoing.numeroSerie ?? t("slot.notRegistered")}
                    </span>
                    <span className="text-muted-foreground">
                      {t("mount.summary.outgoingResult", {
                        brand: outgoing.marca,
                      })}
                    </span>
                  </SummaryRow>
                </>
              ) : null}
            </dl>

            <Alert>
              <InfoIcon />
              <AlertTitle>{t("mount.immutable.title")}</AlertTitle>
              <AlertDescription>
                {t("mount.immutable.description")}
              </AlertDescription>
            </Alert>

            <label
              htmlFor="mount-acknowledge"
              className="flex cursor-pointer items-start gap-3 rounded-2xl border p-3 text-sm has-data-checked:border-primary"
            >
              <Checkbox
                id="mount-acknowledge"
                className="mt-0.5"
                checked={acknowledged}
                onCheckedChange={(checked) =>
                  setAcknowledgedFor(checked ? ackKey : null)
                }
              />
              <span>
                {outgoing
                  ? t("mount.acknowledge.replace", {
                      serial: outgoing.numeroSerie ?? t("slot.notRegistered"),
                    })
                  : t("mount.acknowledge.mount")}
              </span>
            </label>
          </section>
        )}
      </div>

      <DrawerFooter className="flex-row justify-between border-t pt-4">
        {step === "tire" ? (
          <Button type="button" variant="ghost" onClick={onDone}>
            {t("mount.actions.cancel")}
          </Button>
        ) : (
          <Button
            type="button"
            variant="ghost"
            disabled={assign.isPending}
            onClick={() => goTo(step === "confirm" ? "driver" : "tire")}
          >
            <ArrowLeftIcon data-icon="inline-start" />
            {t("mount.actions.back")}
          </Button>
        )}
        <div className="flex gap-2">
          {step === "driver" && driver === null ? (
            <Button
              type="button"
              variant="outline"
              onClick={() => goTo("confirm")}
            >
              {t("mount.actions.skip")}
            </Button>
          ) : null}
          {step === "confirm" ? (
            <Button
              type="button"
              disabled={!acknowledged || !tire || assign.isPending}
              onClick={submit}
            >
              {assign.isPending ? (
                <Loader2Icon
                  data-icon="inline-start"
                  className="animate-spin"
                />
              ) : (
                <CheckIcon data-icon="inline-start" />
              )}
              {assign.isPending
                ? t("mount.actions.submitting")
                : outgoing
                  ? t("mount.actions.confirmReplace")
                  : t("mount.actions.confirm")}
            </Button>
          ) : (
            <Button
              type="button"
              disabled={step === "tire" && tire === null}
              onClick={() =>
                step === "tire" && tire ? chooseTire(tire) : goTo("confirm")
              }
            >
              {t("mount.actions.continue")}
            </Button>
          )}
        </div>
      </DrawerFooter>
    </>
  )
}

/** Monta el buscador compuesto por `router.tsx` como un componente más. */
function TirePickerHost({
  render,
  ...props
}: TirePickerProps & { render: RenderTirePicker }) {
  return render(props)
}

function StepIndicator({ current }: { current: number }) {
  const { t } = useTranslation("transport-units")
  return (
    <ol
      aria-label={t("mount.steps.label")}
      className="flex items-center gap-2 text-xs"
    >
      {STEPS.map((step, index) => (
        <li
          key={step}
          aria-current={index === current ? "step" : undefined}
          className="flex items-center gap-2"
        >
          <span
            className={cn(
              "flex items-center gap-1.5",
              index === current
                ? "font-medium text-foreground"
                : "text-muted-foreground"
            )}
          >
            <span
              aria-hidden="true"
              className={cn(
                "grid size-5 place-items-center rounded-full border text-[0.625rem]",
                index < current &&
                  "border-primary bg-primary text-primary-foreground",
                index === current && "border-primary text-primary"
              )}
            >
              {index < current ? <CheckIcon className="size-3" /> : index + 1}
            </span>
            {t(`mount.steps.${step}.label`)}
          </span>
          {index < STEPS.length - 1 ? (
            <span aria-hidden="true" className="h-px w-4 bg-border" />
          ) : null}
        </li>
      ))}
    </ol>
  )
}

function StepHeading({
  children,
  ref,
}: {
  children: React.ReactNode
  ref?: React.Ref<HTMLHeadingElement>
}) {
  return (
    <h3 ref={ref} tabIndex={-1} className="text-sm font-medium outline-none">
      {children}
    </h3>
  )
}

function SummaryRow({
  label,
  children,
  onChange,
  changeLabel,
}: {
  label: string
  children: React.ReactNode
  onChange?: () => void
  changeLabel?: string
}) {
  const { t } = useTranslation("transport-units")
  return (
    <div className="flex items-start justify-between gap-3">
      <div className="flex min-w-0 flex-col gap-0.5">
        <dt className="text-xs text-muted-foreground">{label}</dt>
        <dd className="flex min-w-0 flex-col gap-0.5">{children}</dd>
      </div>
      {onChange ? (
        <Button
          type="button"
          variant="link"
          size="sm"
          aria-label={changeLabel}
          onClick={onChange}
        >
          {t("mount.summary.change")}
        </Button>
      ) : null}
    </div>
  )
}

/** Conductor opcional, buscado en el servidor por apellido, nombre o DNI. */
function DriverCombobox({
  value,
  onChange,
}: {
  value: DriverOption | null
  onChange: (driver: DriverOption | null) => void
}) {
  const { t } = useTranslation("transport-units")
  const [term, setTerm] = React.useState("")
  const debouncedTerm = useDebouncedValue(term.trim())
  const { data, isFetching, isError } = useDriverSearch(debouncedTerm)
  const drivers = data?.content ?? []

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor="mount-driver" className="text-sm font-medium">
        {t("mount.steps.driver.label")}
      </label>
      <Combobox<DriverOption>
        items={drivers}
        // El filtro lo hace el servidor.
        filter={null}
        value={value}
        onValueChange={(next) => onChange(next ?? null)}
        onInputValueChange={(next) => {
          // Al elegir, el input muestra la etiqueta: no es una búsqueda nueva.
          if (value && next === driverLabel(value)) return
          setTerm(next)
        }}
        itemToStringLabel={driverLabel}
        isItemEqualToValue={(item, selected) =>
          item.publicId === selected.publicId
        }
      >
        <ComboboxInput
          id="mount-driver"
          autoFocus
          showClear={value !== null}
          placeholder={t("mount.steps.driver.placeholder")}
          className="w-full"
        />
        <ComboboxContent>
          <ComboboxEmpty>
            {isError
              ? t("mount.steps.driver.error")
              : isFetching
                ? t("mount.steps.driver.loading")
                : t("mount.steps.driver.empty")}
          </ComboboxEmpty>
          <ComboboxList>
            {(driver: DriverOption) => (
              <ComboboxItem key={driver.publicId} value={driver}>
                <span className="flex min-w-0 flex-col">
                  <span className="truncate">{driverLabel(driver)}</span>
                  {driver.dni ? (
                    <span className="text-xs font-normal text-muted-foreground">
                      {t("mount.steps.driver.dni", { dni: driver.dni })}
                    </span>
                  ) : null}
                </span>
              </ComboboxItem>
            )}
          </ComboboxList>
        </ComboboxContent>
      </Combobox>
    </div>
  )
}
