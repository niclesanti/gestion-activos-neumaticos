import { useQuery } from "@tanstack/react-query"

import { getTire } from "@/features/tires/api/tires-api"

/**
 * Ficha de un neumático. Con `publicId` nulo (dialog cerrado) no consulta; la
 * clave queda bajo `["tires"]`, que el alta invalida.
 */
export function useTire(publicId: string | null) {
  return useQuery({
    queryKey: ["tires", "detail", publicId],
    queryFn: () => getTire(publicId as string),
    enabled: publicId !== null,
    retry: false,
  })
}
