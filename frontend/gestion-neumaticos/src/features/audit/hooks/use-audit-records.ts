import { keepPreviousData, useQuery } from "@tanstack/react-query"

import {
  getAuditRecords,
  isAuditAvailable,
  type AuditedEntity,
  type AuditQuery,
} from "@/features/audit/api/audit-api"

/**
 * Fichas de auditoría de una entidad. Conserva la página anterior mientras llega
 * la nueva (sin parpadeo al filtrar u ordenar) y no consulta mientras la
 * pestaña no está activa o la entidad no tiene backend.
 */
export function useAuditRecords(
  entity: AuditedEntity,
  query: AuditQuery,
  { enabled = true }: { enabled?: boolean } = {}
) {
  return useQuery({
    queryKey: ["audit", entity, query],
    queryFn: () => getAuditRecords(entity, query),
    enabled: enabled && isAuditAvailable(entity),
    placeholderData: keepPreviousData,
  })
}
