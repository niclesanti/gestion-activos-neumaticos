import { apiClient } from "@/lib/api/client"
import type { Page } from "@/lib/api/page"

/** Conductor activo (`ConductorListadoDTO` del backend). */
export type DriverOption = {
  publicId: string
  nombre: string
  apellido: string
  dni: string | null
}

/**
 * Conductores activos por apellido, nombre o DNI, paginados con el tamaño por
 * defecto del backend (que siempre ordena por apellido y nombre).
 */
export async function searchDrivers(q: string, page: number) {
  const { data } = await apiClient.get<Page<DriverOption>>(
    "/api/v1/conductores",
    { params: { q: q || undefined, page } }
  )
  return data
}
