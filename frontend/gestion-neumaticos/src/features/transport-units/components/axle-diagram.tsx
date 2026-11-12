import * as React from "react"

import { ArrowUpIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import type {
  ConfigurationAxle,
  UnitConfiguration,
} from "@/features/transport-units/api/transport-units-api"
import { cn } from "@/lib/utils"

/** Estado de una posición: `description` se suma a la lista para lectores de pantalla. */
export type SlotState = {
  occupied: boolean
  description?: string
}

/** Lo que recibe `renderWheel` para dibujar una posición con el mismo aspecto. */
export type WheelRenderProps = {
  code: string
  /** Undefined cuando el diagrama no conoce el estado (vista previa del alta). */
  occupied: boolean | undefined
  /** Clases del dibujo de la rueda: forma, borde y relleno según el estado. */
  className: string
}

type RenderWheel = (wheel: WheelRenderProps) => React.ReactNode

/**
 * Códigos de las ruedas de un eje, de izquierda a derecha, como los genera
 * `UnidadTransporteFactoryImpl` en el backend.
 */
function wheelCodes(axle: ConfigurationAxle) {
  const prefix = `E${axle.orden}`
  if (axle.rodado === "DUAL") {
    return {
      left: [`${prefix}-IZQ-EXT`, `${prefix}-IZQ-INT`],
      right: [`${prefix}-DER-INT`, `${prefix}-DER-EXT`],
    }
  }
  return { left: [`${prefix}-IZQ`], right: [`${prefix}-DER`] }
}

function wheelClassName(shape: string, occupied: boolean | undefined) {
  return cn(
    "rounded-lg border-2 transition-colors",
    shape,
    occupied
      ? "border-primary bg-primary"
      : "border-foreground/70 bg-background"
  )
}

/**
 * Esquema de ejes y neumáticos de una configuración, como la planilla en
 * papel de la empresa: un eje por fila, de adelante hacia atrás, y los
 * auxilios al final. Con `slotStates` distingue posiciones ocupadas de vacías
 * y muestra la leyenda; con `renderWheel` cada rueda puede ser interactiva
 * (la ficha la usa para abrir los datos del neumático).
 */
export function AxleDiagram({
  configuration,
  slotStates,
  renderWheel,
  className,
}: {
  configuration: UnitConfiguration
  slotStates?: Record<string, SlotState>
  renderWheel?: RenderWheel
  className?: string
}) {
  const { t } = useTranslation("transport-units")
  const spares = Array.from(
    { length: configuration.cantSlotsAuxilio },
    (_, index) => `AUX-${index + 1}`
  )
  const summary = t("preview.summary", {
    axles: configuration.totalEjes,
    slots: configuration.totalSlots,
  })
  const interactive = renderWheel !== undefined

  function drawWheel(code: string, shape: string) {
    const wheel: WheelRenderProps = {
      code,
      occupied: slotStates?.[code]?.occupied,
      className: "",
    }
    wheel.className = wheelClassName(shape, wheel.occupied)
    return renderWheel ? (
      renderWheel(wheel)
    ) : (
      <div className={wheel.className} />
    )
  }

  function axleLabel(axle: ConfigurationAxle) {
    return t("preview.axle", {
      number: axle.orden,
      type: t(`axleTypes.${axle.tipoEje}`),
      wheels: t(`wheelTypes.${axle.rodado}`),
    })
  }

  function codeWithDescription(code: string) {
    const description = slotStates?.[code]?.description
    return description ? `${code} (${description})` : code
  }

  return (
    <figure
      data-slot="axle-diagram"
      className={cn(
        "flex flex-col gap-4 rounded-3xl border bg-muted/30 p-4",
        className
      )}
    >
      <figcaption className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <span className="text-sm font-medium">
          {t("preview.title", { name: configuration.nombre })}
        </span>
        <span className="text-xs text-muted-foreground">{summary}</span>
      </figcaption>

      {/* Sin ruedas interactivas el dibujo es decorativo para lectores de
          pantalla (lo resume el aria-label y detalla la lista oculta); con
          ellas es un grupo, porque un role="img" volvería mudos los botones. */}
      <div
        role={interactive ? "group" : "img"}
        aria-label={summary}
        className="mx-auto flex w-full max-w-sm flex-col gap-4"
      >
        <span className="flex items-center justify-center gap-1 text-xs text-muted-foreground">
          <ArrowUpIcon className="size-3.5" />
          {t("preview.front")}
        </span>

        {configuration.ejes.map((axle) => {
          const { left, right } = wheelCodes(axle)
          return (
            <div key={axle.orden} className="flex flex-col gap-1.5">
              <span className="text-xs text-muted-foreground">
                {axleLabel(axle)}
              </span>
              <div className="flex items-center gap-2">
                <WheelGroup codes={left} draw={drawWheel} />
                <div className="relative flex flex-1 items-center justify-center">
                  <div className="h-2 w-full rounded-full bg-foreground/70" />
                  <span className="absolute grid size-7 place-items-center rounded-full bg-foreground text-xs font-semibold text-background">
                    {axle.orden}
                  </span>
                </div>
                <WheelGroup codes={right} draw={drawWheel} />
              </div>
            </div>
          )
        })}

        {spares.length > 0 ? (
          <div className="flex flex-col gap-1.5 border-t pt-4">
            <span className="text-xs text-muted-foreground">
              {t("preview.spares")}
            </span>
            <div className="flex flex-wrap justify-center gap-3">
              {spares.map((code) => (
                <div key={code} className="flex flex-col items-center gap-1">
                  {drawWheel(code, "h-8 w-16")}
                  <WheelCode code={code} />
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </div>

      {slotStates ? (
        <div className="flex flex-wrap items-center justify-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
          <LegendItem occupied label={t("legend.occupied")} />
          <LegendItem occupied={false} label={t("legend.empty")} />
        </div>
      ) : null}

      <ul className="sr-only">
        {configuration.ejes.map((axle) => {
          const { left, right } = wheelCodes(axle)
          return (
            <li key={axle.orden}>
              {axleLabel(axle)}:{" "}
              {[...left, ...right].map(codeWithDescription).join(", ")}
            </li>
          )
        })}
        {spares.length > 0 ? (
          <li>
            {t("preview.spares")}: {spares.map(codeWithDescription).join(", ")}
          </li>
        ) : null}
      </ul>
    </figure>
  )
}

function WheelGroup({
  codes,
  draw,
}: {
  codes: string[]
  draw: (code: string, shape: string) => React.ReactNode
}) {
  return (
    <div className="flex gap-1">
      {codes.map((code) => (
        <div key={code} className="flex flex-col items-center gap-1">
          {draw(code, "h-14 w-7")}
          <WheelCode code={code} />
        </div>
      ))}
    </div>
  )
}

function WheelCode({ code }: { code: string }) {
  // Sin el prefijo del eje (ya está en la fila): "IZQ-EXT" entra bajo la rueda.
  const label = code.startsWith("AUX") ? code : code.replace(/^E\d+-/, "")
  return (
    <span className="font-mono text-[0.625rem] leading-none text-muted-foreground">
      {label}
    </span>
  )
}

function LegendItem({ occupied, label }: { occupied: boolean; label: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span
        aria-hidden="true"
        className={wheelClassName("h-4 w-2.5", occupied)}
      />
      {label}
    </span>
  )
}
