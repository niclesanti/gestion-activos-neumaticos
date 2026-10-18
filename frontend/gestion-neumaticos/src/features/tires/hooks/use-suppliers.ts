import { useQuery } from "@tanstack/react-query"

import { getSuppliers } from "@/features/tires/api/suppliers-api"

export function useSuppliers({ enabled = true }: { enabled?: boolean } = {}) {
  return useQuery({
    queryKey: ["suppliers"],
    queryFn: getSuppliers,
    staleTime: 5 * 60_000,
    enabled,
  })
}
