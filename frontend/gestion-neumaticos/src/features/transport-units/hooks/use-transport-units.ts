import { keepPreviousData, useQuery } from "@tanstack/react-query"

import {
  searchTransportUnits,
  type TransportUnitsQuery,
} from "@/features/transport-units/api/transport-units-api"

/**
 * Unidades activas, con búsqueda por patente y paginación del servidor. La
 * clave queda bajo `["transport-units"]`, que el alta invalida; conserva la
 * página anterior mientras llega la nueva.
 */
export function useTransportUnits(query: TransportUnitsQuery) {
  return useQuery({
    queryKey: ["transport-units", "list", query],
    queryFn: () => searchTransportUnits(query),
    placeholderData: keepPreviousData,
  })
}
