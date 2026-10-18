import { useMutation, useQueryClient } from "@tanstack/react-query"

import { registerTire } from "@/features/tires/api/tires-api"

export function useRegisterTire() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: registerTire,
    // Para el futuro listado de neumáticos.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["tires"] }),
  })
}
