import { useQuery } from "@tanstack/react-query"

import { getTransportUnit } from "@/features/transport-units/api/transport-units-api"

/**
 * Ficha de una unidad. Con `publicId` nulo (dialog cerrado) no consulta; la
 * clave queda bajo `["transport-units"]`, que el alta invalida.
 */
export function useTransportUnit(publicId: string | null) {
  return useQuery({
    queryKey: ["transport-units", "detail", publicId],
    queryFn: () => getTransportUnit(publicId as string),
    enabled: publicId !== null,
    retry: false,
  })
}
