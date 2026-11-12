import type { UnitConfiguration } from "@/features/transport-units/api/transport-units-api"
import { AxleDiagram } from "@/features/transport-units/components/axle-diagram"

/**
 * Esquema de ejes de una configuración, sin neumáticos: lo que se ve al
 * elegirla en el alta. Envuelve a `AxleDiagram` sin estado de posiciones.
 */
export function AxleLayoutPreview({
  configuration,
  className,
}: {
  configuration: UnitConfiguration
  className?: string
}) {
  return <AxleDiagram configuration={configuration} className={className} />
}
