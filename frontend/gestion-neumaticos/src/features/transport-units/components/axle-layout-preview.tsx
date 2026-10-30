import { ArrowUpIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import type {
  ConfigurationAxle,
  UnitConfiguration,
} from "@/features/transport-units/api/transport-units-api"
import { cn } from "@/lib/utils"

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

/**
 * Esquema de ejes y neumáticos de una configuración, como la planilla en
 * papel de la empresa: un eje por fila, de adelante hacia atrás, y los
 * auxilios al final. Cada rueda lleva el código de la posición que se va a
 * generar al registrar la unidad.
 */
export function AxleLayoutPreview({
  configuration,
  className,
}: {
  configuration: UnitConfiguration
  className?: string
}) {
  const { t } = useTranslation("transport-units")
  const spares = Array.from(
    { length: configuration.cantSlotsAuxilio },
    (_, index) => `AUX-${index + 1}`
  )

  return (
    <figure
      data-slot="axle-layout-preview"
      className={cn(
        "flex flex-col gap-4 rounded-3xl border bg-muted/30 p-4",
        className
      )}
    >
      <figcaption className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <span className="text-sm font-medium">
          {t("preview.title", { name: configuration.nombre })}
        </span>
        <span className="text-xs text-muted-foreground">
          {t("preview.summary", {
            axles: configuration.totalEjes,
            slots: configuration.totalSlots,
          })}
        </span>
      </figcaption>

      {/* El dibujo es decorativo para lectores de pantalla: lo resume el
          aria-label (el detalle por eje está en la lista oculta de abajo). */}
      <div
        role="img"
        aria-label={t("preview.summary", {
          axles: configuration.totalEjes,
          slots: configuration.totalSlots,
        })}
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
                {t("preview.axle", {
                  number: axle.orden,
                  type: t(`axleTypes.${axle.tipoEje}`),
                  wheels: t(`wheelTypes.${axle.rodado}`),
                })}
              </span>
              <div className="flex items-center gap-2">
                <WheelGroup codes={left} />
                <div className="relative flex flex-1 items-center justify-center">
                  <div className="h-2 w-full rounded-full bg-foreground/70" />
                  <span className="absolute grid size-7 place-items-center rounded-full bg-foreground text-xs font-semibold text-background">
                    {axle.orden}
                  </span>
                </div>
                <WheelGroup codes={right} />
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
                  <div className="h-8 w-16 rounded-lg border-2 border-foreground/70 bg-background" />
                  <WheelCode code={code} />
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </div>

      <ul className="sr-only">
        {configuration.ejes.map((axle) => {
          const { left, right } = wheelCodes(axle)
          return (
            <li key={axle.orden}>
              {t("preview.axle", {
                number: axle.orden,
                type: t(`axleTypes.${axle.tipoEje}`),
                wheels: t(`wheelTypes.${axle.rodado}`),
              })}
              : {[...left, ...right].join(", ")}
            </li>
          )
        })}
        {spares.length > 0 ? (
          <li>
            {t("preview.spares")}: {spares.join(", ")}
          </li>
        ) : null}
      </ul>
    </figure>
  )
}

function WheelGroup({ codes }: { codes: string[] }) {
  return (
    <div className="flex gap-1">
      {codes.map((code) => (
        <div key={code} className="flex flex-col items-center gap-1">
          <div className="h-14 w-7 rounded-lg border-2 border-foreground/70 bg-background" />
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
