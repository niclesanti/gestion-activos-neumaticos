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

/**
 * Única fuente de verdad de qué niveles ven cada sección: la consumen la
 * navegación (para ocultar el ítem) y el router (para bloquear la URL). Una
 * ruta ausente está abierta a cualquier usuario autenticado.
 *
 * Es autorización de interfaz: la barrera real es el backend.
 */
export const ROUTE_ACCESS: Partial<Record<AppRoute, readonly AccessLevel[]>> = {
  [ROUTES.settings]: ["ROLE_ADMINISTRADOR"],
  [ROUTES.audit]: ["ROLE_ADMINISTRADOR"],
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
