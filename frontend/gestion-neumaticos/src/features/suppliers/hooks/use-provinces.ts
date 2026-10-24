import { useQuery } from "@tanstack/react-query"

import { getProvinces } from "@/features/suppliers/api/suppliers-api"

/** Catálogo fijo (lo cargan las migraciones): no hace falta refrescarlo. */
export function useProvinces() {
  return useQuery({
    queryKey: ["provinces"],
    queryFn: getProvinces,
    staleTime: Infinity,
  })
}
