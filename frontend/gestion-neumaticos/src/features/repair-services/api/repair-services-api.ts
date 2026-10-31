import type { RepairServiceFormValues } from "@/features/repair-services/schemas/repair-service-schema"
import type { Address } from "@/lib/address"
import { apiClient } from "@/lib/api/client"

/** Servicio de reparación (`ServicioReparacionResponseDTO` del backend). */
export type RepairService = {
  publicId: string
  nombre: string
  email: string | null
  telefono: string | null
  cantidadNeumaticosEnReparacion: number
  domicilio: Address | null
}

export async function registerRepairService(values: RepairServiceFormValues) {
  const { data } = await apiClient.post<RepairService>(
    "/api/v1/servicios-reparacion",
    values
  )
  return data
}
