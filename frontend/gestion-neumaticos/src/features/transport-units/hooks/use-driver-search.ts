import { keepPreviousData, useQuery } from "@tanstack/react-query"

import { searchDrivers } from "@/features/transport-units/api/drivers-api"

/**
 * Conductores para asociar al montaje, buscados y paginados en el servidor
 * (son cientos). Conserva la página anterior mientras llega la nueva.
 */
export function useDriverSearch(term: string, page: number) {
  return useQuery({
    queryKey: ["transport-units", "drivers", term, page],
    queryFn: () => searchDrivers(term, page),
    placeholderData: keepPreviousData,
  })
}
