import { useQuery } from "@tanstack/react-query"

import { getUnitConfigurations } from "@/features/transport-units/api/transport-units-api"

/** Las configuraciones solo cambian por migraciones: no hace falta refrescarlas. */
export function useUnitConfigurations() {
  return useQuery({
    queryKey: ["transport-units", "configurations"],
    queryFn: getUnitConfigurations,
    staleTime: Infinity,
  })
}
