import type { AuditStatusFilter } from "@/features/audit/api/audit-api"

/** Filtros de la barra de búsqueda de la auditoría. */
export type AuditFilters = {
  q: string
  estado: AuditStatusFilter
  /** `YYYY-MM-DD`, vacío = sin límite. */
  desde: string
  hasta: string
}

export const EMPTY_AUDIT_FILTERS: AuditFilters = {
  q: "",
  estado: "TODOS",
  desde: "",
  hasta: "",
}

export function hasActiveFilters(filters: AuditFilters) {
  return (
    filters.q !== "" ||
    filters.estado !== "TODOS" ||
    filters.desde !== "" ||
    filters.hasta !== ""
  )
}
