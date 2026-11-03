import { useMutation, useQueryClient } from "@tanstack/react-query"

import { registerStorageCenter } from "@/features/storage-centers/api/storage-centers-api"

export function useRegisterStorageCenter() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: registerStorageCenter,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: ["storage-centers"] }),
  })
}
