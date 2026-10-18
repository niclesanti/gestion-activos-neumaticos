import type {
  AxleType,
  TireFormValues,
  TireStatus,
} from "@/features/tires/schemas/tire-schema"
import { apiClient } from "@/lib/api/client"

/** Marca de neumático (`MarcaNeumaticoResponseDTO` del backend). */
export type TireBrand = {
  id: number
  nombre: string
}

/** Neumático (`NeumaticoResponseDTO` del backend). */
export type Tire = {
  publicId: string
  numeroSerie: string | null
  estadoActual: TireStatus
  enEspera: boolean
  kilometrajeTotal: number | null
  kilometrajeUltimaReparacion: number | null
  tipoOrigen: AxleType
  tipoActual: AxleType
  cantidadRecapados: number | null
  situacionOperativaVigente: string
  comentario: string | null
  marcaNombre: string
}

export async function registerTire(values: TireFormValues) {
  const { data } = await apiClient.post<Tire>("/api/neumatico", values)
  return data
}

export async function getTireBrands() {
  const { data } = await apiClient.get<TireBrand[]>("/api/neumatico/marcas")
  return data
}
