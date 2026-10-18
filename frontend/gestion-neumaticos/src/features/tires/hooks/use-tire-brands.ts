import { useQuery } from "@tanstack/react-query"

import { getTireBrands } from "@/features/tires/api/tires-api"

/** Las marcas solo cambian por migraciones: no hace falta refrescarlas seguido. */
export function useTireBrands() {
  return useQuery({
    queryKey: ["tires", "brands"],
    queryFn: getTireBrands,
    staleTime: 5 * 60_000,
  })
}
