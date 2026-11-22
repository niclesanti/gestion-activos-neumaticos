import { keepPreviousData, useQuery } from "@tanstack/react-query"

import { searchTires, type TiresQuery } from "@/features/tires/api/tires-api"

/**
 * Neumáticos activos, con filtros y paginación del servidor. La clave queda
 * bajo `["tires"]`, que el alta invalida; conserva la página anterior mientras
 * llega la nueva.
 */
export function useTires(query: TiresQuery) {
  return useQuery({
    queryKey: ["tires", "list", query],
    queryFn: () => searchTires(query),
    placeholderData: keepPreviousData,
  })
}
