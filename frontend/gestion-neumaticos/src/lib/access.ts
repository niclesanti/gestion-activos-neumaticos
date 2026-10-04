import { ROUTES, type AppRoute } from "@/lib/routes"

/**
 * Niveles de acceso del sistema, tal como los emite el backend
 * (`NivelAcceso` en el módulo `usuarios`).
 */
export const ACCESS_LEVELS = [
  "ROLE_ADMINISTRADOR",
  "ROLE_EDITOR",
  "ROLE_LECTOR",
] as const

export type AccessLevel = (typeof ACCESS_LEVELS)[number]

const ADMIN_ONLY: readonly AccessLevel[] = ["ROLE_ADMINISTRADOR"]
const ADMIN_AND_EDITOR: readonly AccessLevel[] = [
  "ROLE_ADMINISTRADOR",
  "ROLE_EDITOR",
]

/**
 * Única fuente de verdad de qué niveles ven cada sección: la consumen la
 * navegación (para ocultar el ítem) y el router (para bloquear la URL). Una
 * ruta ausente está abierta a cualquier usuario autenticado.
 *
 * - Administrador: todo.
 * - Editor: todo menos Configuración y Auditoría.
 * - Lector: por ahora solo Inicio (sus permisos de lectura están pendientes).
 *
 * Es autorización de interfaz: la barrera real es el backend.
 */
export const ROUTE_ACCESS: Partial<Record<AppRoute, readonly AccessLevel[]>> = {
  [ROUTES.transportUnits]: ADMIN_AND_EDITOR,
  [ROUTES.tires]: ADMIN_AND_EDITOR,
  [ROUTES.repairs]: ADMIN_AND_EDITOR,
  [ROUTES.storage]: ADMIN_AND_EDITOR,
  [ROUTES.settings]: ADMIN_ONLY,
  [ROUTES.audit]: ADMIN_ONLY,
}

export function canAccess(
  level: AccessLevel | null | undefined,
  allowed?: readonly AccessLevel[]
) {
  if (!allowed) {
    return true
  }
  return level != null && allowed.includes(level)
}

/** ¿Puede el nivel dado entrar a la ruta? Acepta cualquier pathname. */
export function canAccessRoute(
  level: AccessLevel | null | undefined,
  pathname: string
) {
  return canAccess(level, ROUTE_ACCESS[pathname as AppRoute])
}
