import { useQuery } from "@tanstack/react-query"

import { getStorageCenters } from "@/features/tires/api/storage-centers-api"

/**
 * Centros de almacenamiento para filtrar por origen. Bajo `["tires"]`: el
 * montaje invalida esa clave y el stock de cada centro se actualiza.
 */
export function useStorageCenters() {
  return useQuery({
    queryKey: ["tires", "storage-centers"],
    queryFn: getStorageCenters,
  })
}
