import type { StorageCenterFormValues } from "@/features/storage-centers/schemas/storage-center-schema"
import type { Address } from "@/lib/address"
import { apiClient } from "@/lib/api/client"

/** Centro de almacenamiento (`CentroAlmacenamientoResponseDTO` del backend). */
export type StorageCenter = {
  publicId: string
  nombre: string
  cantidadNeumaticosEnStock: number
  domicilio: Address
}

export async function registerStorageCenter(values: StorageCenterFormValues) {
  const { data } = await apiClient.post<StorageCenter>(
    "/api/v1/centros-almacenamiento",
    values
  )
  return data
}
