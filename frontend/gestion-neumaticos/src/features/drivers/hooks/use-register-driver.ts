import { useMutation, useQueryClient } from "@tanstack/react-query"

import { registerDriver } from "@/features/drivers/api/drivers-api"

export function useRegisterDriver() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: registerDriver,
    // Para el futuro listado de conductores.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["drivers"] }),
  })
}
