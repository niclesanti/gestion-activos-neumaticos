import { useMutation, useQueryClient } from "@tanstack/react-query"

import { registerSupplier } from "@/features/suppliers/api/suppliers-api"

export function useRegisterSupplier() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: registerSupplier,
    // Misma clave que el select de proveedores del alta de neumático.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["suppliers"] }),
  })
}
