import { apiClient } from "@/lib/api/client"

/** Centro activo con su stock (`CentroAlmacenamientoListadoDTO` del backend). */
export type StorageCenterOption = {
  publicId: string
  nombre: string
  cantidadNeumaticosEnStock: number
}

/** Centros activos ordenados por nombre (sin paginar: son pocos). */
export async function getStorageCenters() {
  const { data } = await apiClient.get<StorageCenterOption[]>(
    "/api/v1/centros-almacenamiento"
  )
  return data
}
