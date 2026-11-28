import { useMutation, useQueryClient } from "@tanstack/react-query"

import { unassignTire } from "@/features/transport-units/api/transport-units-api"

/**
 * Desmonta el neumático de una posición. Al terminar guarda la ficha devuelta
 * y refresca el resto de `["transport-units"]` (salvo las configuraciones) y los
 * neumáticos, cuyo listado y situación cambiaron.
 */
export function useUnassignTire(unitPublicId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (slotCode: string) => unassignTire(unitPublicId, slotCode),
    onSuccess: async (unit) => {
      queryClient.setQueryData(
        ["transport-units", "detail", unitPublicId],
        unit
      )
      await Promise.all([
        queryClient.invalidateQueries({
          queryKey: ["transport-units"],
          predicate: (query) => query.queryKey[1] !== "configurations",
        }),
        queryClient.invalidateQueries({ queryKey: ["tires"] }),
      ])
    },
  })
}
