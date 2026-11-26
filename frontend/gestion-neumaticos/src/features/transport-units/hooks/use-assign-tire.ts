import { useMutation, useQueryClient } from "@tanstack/react-query"

import {
  assignTire,
  type AssignTireBody,
} from "@/features/transport-units/api/transport-units-api"

/**
 * Monta un neumático en una posición. Al terminar refresca la ficha (y el
 * resto de `["transport-units"]` salvo las configuraciones) y los neumáticos,
 * cuyo listado, situación y stock por centro cambiaron.
 */
export function useAssignTire(unitPublicId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({
      slotCode,
      body,
    }: {
      slotCode: string
      body: AssignTireBody
    }) => assignTire(unitPublicId, slotCode, body),
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
