import type { ParseKeys } from "i18next"
import {
  BellIcon,
  CircleDotIcon,
  HomeIcon,
  ScrollTextIcon,
  SettingsIcon,
  TruckIcon,
  WarehouseIcon,
  WrenchIcon,
} from "lucide-react"

import { canAccessRoute, type AccessLevel } from "@/lib/access"
import { ROUTES, type AppRoute } from "@/lib/routes"

/** Claves de traducción válidas del namespace `common`. */
export type NavLabelKey = ParseKeys<"common">

/** Identificador estable de cada grupo: se filtra por id y no por posición. */
export type NavGroupId = "platform" | "operations" | "system"

export type NavItem = {
  to: AppRoute
  /** Clave del namespace `common` de i18next. */
  labelKey: NavLabelKey
  icon: React.ElementType
}

export type NavGroup = {
  id: NavGroupId
  labelKey: NavLabelKey
  items: NavItem[]
}

/**
 * Única fuente de verdad de la navegación: la consumen la sidebar, el breadcrumb
 * y el menú inferior de mobile, para que no puedan quedar desincronizados.
 *
 * El orden de los grupos sigue la frecuencia de uso: primero el inicio, después
 * las operaciones del día a día y al final lo administrativo. Los ABM de
 * entidades poco frecuentes no viven acá sino dentro de /settings.
 */
export const NAV_GROUPS: NavGroup[] = [
  {
    id: "platform",
    labelKey: "nav.group.platform",
    items: [{ to: ROUTES.home, labelKey: "nav.home", icon: HomeIcon }],
  },
  {
    id: "operations",
    labelKey: "nav.group.operations",
    items: [
      {
        to: ROUTES.transportUnits,
        labelKey: "nav.transportUnits",
        icon: TruckIcon,
      },
      { to: ROUTES.tires, labelKey: "nav.tires", icon: CircleDotIcon },
      { to: ROUTES.repairs, labelKey: "nav.repairs", icon: WrenchIcon },
      { to: ROUTES.storage, labelKey: "nav.storage", icon: WarehouseIcon },
    ],
  },
  {
    id: "system",
    labelKey: "nav.group.system",
    items: [
      { to: ROUTES.settings, labelKey: "nav.settings", icon: SettingsIcon },
      { to: ROUTES.audit, labelKey: "nav.audit", icon: ScrollTextIcon },
      // Showcase de notificaciones: no existe en el build de producción.
      ...(import.meta.env.DEV
        ? ([
            { to: ROUTES.toasts, labelKey: "nav.toasts", icon: BellIcon },
          ] satisfies NavItem[])
        : []),
    ],
  },
]

/** Ruta de inicio del shell: primer tramo de todo breadcrumb. */
export const HOME_PATH = ROUTES.home

/** Mapa ruta → clave de etiqueta, derivado de NAV_GROUPS. */
export const ROUTE_LABELS: Record<string, NavLabelKey> = Object.fromEntries(
  NAV_GROUPS.flatMap((group) =>
    group.items.map((item) => [item.to, item.labelKey])
  )
)

/** Grupos con acceso directo en el menú flotante inferior (mobile-only). */
const MOBILE_NAV_GROUP_IDS: NavGroupId[] = ["platform", "operations"]

/**
 * Accesos rápidos para el menú flotante inferior (mobile-only): se derivan por
 * id de grupo, así reordenar NAV_GROUPS no cambia el menú en silencio.
 */
export const MOBILE_NAV_ITEMS: NavItem[] = NAV_GROUPS.filter((group) =>
  MOBILE_NAV_GROUP_IDS.includes(group.id)
).flatMap((group) => group.items)

/**
 * Navegación visible para un nivel de acceso (reglas en `ROUTE_ACCESS`): quita
 * los ítems no permitidos y los grupos que quedan vacíos.
 */
export function filterNavGroups(
  groups: NavGroup[],
  level: AccessLevel | null | undefined
): NavGroup[] {
  return groups
    .map((group) => ({
      ...group,
      items: filterNavItems(group.items, level),
    }))
    .filter((group) => group.items.length > 0)
}

export function filterNavItems(
  items: NavItem[],
  level: AccessLevel | null | undefined
): NavItem[] {
  return items.filter((item) => canAccessRoute(level, item.to))
}
