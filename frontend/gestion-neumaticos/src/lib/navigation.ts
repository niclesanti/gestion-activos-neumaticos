import type { ParseKeys } from "i18next"
import {
  CircleDotIcon,
  HomeIcon,
  ScrollTextIcon,
  SettingsIcon,
  TruckIcon,
  WarehouseIcon,
  WrenchIcon,
} from "lucide-react"

/** Claves de traducción válidas del namespace `common`. */
export type NavLabelKey = ParseKeys<"common">

export type NavItem = {
  to: string
  /** Clave del namespace `common` de i18next. */
  labelKey: NavLabelKey
  icon: React.ElementType
}

export type NavGroup = {
  labelKey: NavLabelKey
  items: NavItem[]
}

/**
 * Única fuente de verdad de la navegación: la consumen tanto la sidebar como
 * el breadcrumb, para que no puedan quedar desincronizados.
 *
 * El orden de los grupos sigue la frecuencia de uso: primero el inicio, después
 * las operaciones del día a día y al final lo administrativo. Los ABM de
 * entidades poco frecuentes no viven acá sino dentro de /settings.
 */
export const NAV_GROUPS: NavGroup[] = [
  {
    labelKey: "nav.group.platform",
    items: [{ to: "/home", labelKey: "nav.home", icon: HomeIcon }],
  },
  {
    labelKey: "nav.group.operations",
    items: [
      {
        to: "/transport-units",
        labelKey: "nav.transportUnits",
        icon: TruckIcon,
      },
      { to: "/tires", labelKey: "nav.tires", icon: CircleDotIcon },
      { to: "/repairs", labelKey: "nav.repairs", icon: WrenchIcon },
      { to: "/storage", labelKey: "nav.storage", icon: WarehouseIcon },
    ],
  },
  {
    labelKey: "nav.group.system",
    items: [
      { to: "/settings", labelKey: "nav.settings", icon: SettingsIcon },
      { to: "/audit", labelKey: "nav.audit", icon: ScrollTextIcon },
    ],
  },
]

/** Ruta de inicio del shell: primer tramo de todo breadcrumb. */
export const HOME_PATH = "/home"

/** Mapa ruta → clave de etiqueta, derivado de NAV_GROUPS. */
export const ROUTE_LABELS: Record<string, NavLabelKey> = Object.fromEntries(
  NAV_GROUPS.flatMap((group) =>
    group.items.map((item) => [item.to, item.labelKey])
  )
)
