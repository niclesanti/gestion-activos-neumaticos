import type { TransportUnitFormValues } from "@/features/transport-units/schemas/transport-unit-schema"
import { apiClient } from "@/lib/api/client"

export type UnitType = "TRACTOR" | "SEMIRREMOLQUE"
export type AxleType = "DIRECCION" | "TRACCION" | "ARRASTRE"
export type WheelType = "SIMPLE" | "DUAL"
export type SlotType = "EJE" | "AUXILIO"
export type Side = "IZQUIERDO" | "DERECHO"
export type WheelPosition = "SIMPLE" | "EXTERIOR" | "INTERIOR"

/** Eje de una configuración (`EjeConfiguracionDTO` del backend). */
export type ConfigurationAxle = {
  orden: number
  tipoEje: AxleType
  rodado: WheelType
  cantidadNeumaticos: number
}

/** Configuración de ejes precargada (`ConfiguracionUnidadDTO` del backend). */
export type UnitConfiguration = {
  id: number
  nombre: string
  tipoUnidad: UnitType
  cantSlotsAuxilio: number
  totalEjes: number
  totalSlots: number
  ejes: ConfigurationAxle[]
}

/** Posición de una unidad (`SlotUnidadTransporteDTO` del backend). */
export type UnitSlot = {
  codigo: string
  tipoSlot: SlotType
  nroEje: number | null
  tipoEje: AxleType | null
  lado: Side | null
  posicionRueda: WheelPosition | null
  nroAuxilio: number | null
  ocupado: boolean
}

/** Unidad de transporte (`UnidadTransporteResponseDTO` del backend). */
export type TransportUnit = {
  publicId: string
  patente: string
  configuracion: { id: number; nombre: string; tipoUnidad: UnitType }
  slots: UnitSlot[]
}

export async function registerTransportUnit(values: TransportUnitFormValues) {
  const { data } = await apiClient.post<TransportUnit>(
    "/api/v1/unidades-transporte",
    values
  )
  return data
}

export async function getUnitConfigurations() {
  const { data } = await apiClient.get<UnitConfiguration[]>(
    "/api/v1/unidades-transporte/configuraciones"
  )
  return data
}
