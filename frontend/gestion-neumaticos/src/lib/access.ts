import * as React from "react"

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
 * - Editor: todo menos Auditoría.
 * - Lector: Inicio y Configuración (todas las pestañas menos Usuarios, en
 *   solo lectura); el resto de sus permisos de lectura está pendiente.
 *
 * Es autorización de interfaz: la barrera real es el backend.
 */
export const ROUTE_ACCESS: Partial<Record<AppRoute, readonly AccessLevel[]>> = {
  [ROUTES.transportUnits]: ADMIN_AND_EDITOR,
  [ROUTES.tires]: ADMIN_AND_EDITOR,
  [ROUTES.repairs]: ADMIN_AND_EDITOR,
  [ROUTES.storage]: ADMIN_AND_EDITOR,
  [ROUTES.audit]: ADMIN_ONLY,
}

/** Pestañas (catálogos) de Configuración. */
export const SETTINGS_TABS = [
  "users",
  "drivers",
  "suppliers",
  "repair-services",
  "storage-centers",
] as const

export type SettingsTab = (typeof SETTINGS_TABS)[number]

/**
 * Qué niveles ven cada pestaña de Configuración. Usuarios es solo del
 * administrador; el resto de los catálogos los ven todos (qué puede hacer cada
 * uno adentro lo decide `ACTION_ACCESS`).
 */
export const SETTINGS_TAB_ACCESS: Record<SettingsTab, readonly AccessLevel[]> =
  {
    users: ADMIN_ONLY,
    drivers: ACCESS_LEVELS,
    suppliers: ACCESS_LEVELS,
    "repair-services": ACCESS_LEVELS,
    "storage-centers": ACCESS_LEVELS,
  }

/**
 * Acciones de negocio que la interfaz muestra u oculta según el nivel. Una
 * acción nueva se agrega acá, con la misma regla que su `@PreAuthorize`:
 *
 * - Usuarios: todo el CRUD, solo el administrador.
 * - Resto de las entidades: alta, modificación y baja, administrador y
 *   editor; buscar y filtrar, los tres (el lector es de solo lectura).
 */
export const ACTION_ACCESS = {
  "drivers.create": ADMIN_AND_EDITOR,
} as const satisfies Record<string, readonly AccessLevel[]>

export type AppAction = keyof typeof ACTION_ACCESS

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

/** ¿Puede el nivel dado ver la pestaña de Configuración? */
export function canAccessSettingsTab(
  level: AccessLevel | null | undefined,
  tab: SettingsTab
) {
  return canAccess(level, SETTINGS_TAB_ACCESS[tab])
}

/** ¿Puede el nivel dado hacer la acción? */
export function canPerform(
  level: AccessLevel | null | undefined,
  action: AppAction
) {
  return canAccess(level, ACTION_ACCESS[action])
}

/**
 * Nivel de acceso de la sesión para los features, que no pueden importar
 * `features/auth`: lo provee el shell (`AppLayout`), que sí lee la sesión.
 */
export const AccessLevelContext = React.createContext<AccessLevel | null>(null)

export function useAccessLevel() {
  return React.useContext(AccessLevelContext)
}
