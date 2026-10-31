import { useMutation, useQueryClient } from "@tanstack/react-query"

import { registerRepairService } from "@/features/repair-services/api/repair-services-api"

export function useRegisterRepairService() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: registerRepairService,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["repair-services"] }),
  })
}
