import { keepPreviousData, useQuery } from "@tanstack/react-query"

import { searchDrivers } from "@/features/transport-units/api/drivers-api"
import { DEFAULT_PAGE_SIZE } from "@/lib/api/page"

/**
 * Conductores para asociar al montaje, buscados en el servidor (son cientos).
 * Conserva el resultado anterior mientras se tipea.
 */
export function useDriverSearch(term: string) {
  return useQuery({
    queryKey: ["transport-units", "drivers", term],
    queryFn: () => searchDrivers(term, DEFAULT_PAGE_SIZE),
    placeholderData: keepPreviousData,
  })
}
