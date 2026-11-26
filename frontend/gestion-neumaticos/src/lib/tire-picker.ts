import * as React from "react"

/**
 * Contrato entre el buscador de neumáticos (feature `tires`) y el montaje en
 * una posición (feature `transport-units`, HU-31). Un feature no puede
 * importar otro: `router.tsx` compone el buscador y lo pasa a la página de
 * unidades, que lo expone con este contexto.
 */

/** Neumático disponible elegido en el buscador (en stock o en espera). */
export type PickedTire = {
  publicId: string
  numeroSerie: string | null
  marca: string
  /** Estado técnico (`EstadoNeumatico` del backend). */
  estadoActual: string
  situacion: "EN_STOCK" | "EN_ESPERA"
  /** Nombre del centro si está en stock; null si está en espera. */
  centroAlmacenamiento: string | null
}

export type TirePickerProps = {
  /** Neumático marcado (para volver a este paso sin perder la elección). */
  selected: PickedTire | null
  /** Se marcó un neumático (con el mouse, el teclado o el lector de pantalla). */
  onSelect: (tire: PickedTire) => void
  /** Se eligió con un clic o Enter: el montaje avanza al paso siguiente. */
  onConfirm: (tire: PickedTire) => void
}

export type RenderTirePicker = (props: TirePickerProps) => React.ReactNode

export const TirePickerContext = React.createContext<RenderTirePicker | null>(
  null
)

/** El buscador compuesto por `router.tsx`; null si la página no lo recibió. */
export function useTirePicker() {
  return React.useContext(TirePickerContext)
}
