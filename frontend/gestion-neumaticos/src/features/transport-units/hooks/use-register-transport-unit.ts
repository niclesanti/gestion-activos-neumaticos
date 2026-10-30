import { useMutation, useQueryClient } from "@tanstack/react-query"

import { registerTransportUnit } from "@/features/transport-units/api/transport-units-api"

export function useRegisterTransportUnit() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: registerTransportUnit,
    // Las configuraciones no cambian con un alta: solo se invalidan las unidades.
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: ["transport-units"],
        predicate: (query) => query.queryKey[1] !== "configurations",
      }),
  })
}
