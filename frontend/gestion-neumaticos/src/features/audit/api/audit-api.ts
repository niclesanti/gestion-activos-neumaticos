import { apiClient } from "@/lib/api/client"
import type { Page } from "@/lib/api/page"

/** Entidades con ficha de auditoría, una pestaña de la página por cada una. */
export const AUDITED_ENTITIES = [
  "users",
  "transport-units",
  "repair-services",
  "suppliers",
  "tires",
  "drivers",
  "storage-centers",
] as const

export type AuditedEntity = (typeof AUDITED_ENTITIES)[number]

/**
 * `GET /api/v1/<recurso>/auditoria` de cada módulo. Una entidad sin ruta (`null`)
 * queda sin consulta y su pestaña muestra el estado "no disponible".
 */
const AUDIT_ENDPOINTS: Record<AuditedEntity, string | null> = {
  users: "/api/v1/usuarios/auditoria",
  "transport-units": "/api/v1/unidades-transporte/auditoria",
  "repair-services": "/api/v1/servicios-reparacion/auditoria",
  suppliers: "/api/v1/proveedores/auditoria",
  tires: "/api/v1/neumaticos/auditoria",
  drivers: "/api/v1/conductores/auditoria",
  "storage-centers": "/api/v1/centros-almacenamiento/auditoria",
}

export function isAuditAvailable(entity: AuditedEntity) {
  return AUDIT_ENDPOINTS[entity] !== null
}

/** Usuario que firmó una operación (`AuditorDTO`); los nombres son null si ya no existe. */
export type Auditor = {
  publicId: string
  nombreUsuario: string | null
  nombreApellido: string | null
}

/** Ficha de auditoría de un registro (`RegistroAuditoriaDTO`). */
export type AuditRecord = {
  publicId: string
  descripcion: string
  creadoEn: string
  creadoPor: Auditor | null
  modificadoEn: string
  modificadoPor: Auditor | null
  dadoDeBajaEn: string | null
  dadoDeBajaPor: Auditor | null
  estado: "ACTIVO" | "BAJA"
}

export type AuditStatusFilter = "TODOS" | "ACTIVOS" | "BAJAS"
export type AuditSortField = "createdAt" | "updatedAt"
export type AuditSortDirection = "asc" | "desc"

export type AuditQuery = {
  q: string
  estado: AuditStatusFilter
  /** `YYYY-MM-DD`, vacío = sin límite. */
  desde: string
  hasta: string
  page: number
  size: number
  sort: AuditSortField
  direction: AuditSortDirection
}

export const DEFAULT_AUDIT_QUERY: AuditQuery = {
  q: "",
  estado: "TODOS",
  desde: "",
  hasta: "",
  page: 0,
  size: 20,
  sort: "updatedAt",
  direction: "desc",
}

export async function getAuditRecords(
  entity: AuditedEntity,
  query: AuditQuery
) {
  const endpoint = AUDIT_ENDPOINTS[entity]
  if (!endpoint) {
    throw new Error(`La entidad ${entity} todavía no tiene auditoría`)
  }
  const { data } = await apiClient.get<Page<AuditRecord>>(endpoint, {
    params: {
      q: query.q || undefined,
      estado: query.estado === "TODOS" ? undefined : query.estado,
      desde: query.desde || undefined,
      hasta: query.hasta || undefined,
      page: query.page,
      size: query.size,
      sort: `${query.sort},${query.direction}`,
    },
  })
  return data
}
