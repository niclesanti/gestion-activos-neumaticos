import { useQuery } from "@tanstack/react-query"

import { apiClient } from "@/lib/api/client"

/** Provincia (`ProvinciaResponseDTO` del backend). */
export type Province = {
  id: number
  nombre: string
}

export async function getProvinces() {
  const { data } = await apiClient.get<Province[]>("/api/v1/provincias")
  return data
}

/** Catálogo fijo (lo cargan las migraciones): no hace falta refrescarlo. */
export function useProvinces() {
  return useQuery({
    queryKey: ["provinces"],
    queryFn: getProvinces,
    staleTime: Infinity,
  })
}
