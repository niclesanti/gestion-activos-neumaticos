import type { DriverFormValues } from "@/features/drivers/schemas/driver-schema"
import { apiClient } from "@/lib/api/client"

/** Conductor (`ConductorResponseDTO` del backend). */
export type Driver = {
  publicId: string
  dni: string | null
  nombre: string
  apellido: string
}

export async function registerDriver(values: DriverFormValues) {
  const { data } = await apiClient.post<Driver>("/api/v1/conductores", values)
  return data
}
