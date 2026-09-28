import { useTranslation } from "react-i18next"
import { Link, useLocation } from "react-router-dom"

import { MOBILE_NAV_ITEMS } from "@/lib/navigation"
import { cn } from "@/lib/utils"

/**
 * Menú flotante inferior, visible solo en mobile (`md:hidden`), con acceso
 * directo a las secciones de uso más frecuente. Complementa a la sidebar
 * (que en mobile es un drawer que requiere un tap extra para abrirse).
 */
export function MobileBottomNav() {
  const { t } = useTranslation()
  const { pathname } = useLocation()

  return (
    <nav
      aria-label={t("nav.mobileNav")}
      className="fixed inset-x-4 bottom-4 z-50 pb-[env(safe-area-inset-bottom)] md:hidden"
    >
      <ul className="flex items-center justify-between gap-1 rounded-full border border-border bg-muted/95 p-1.5 shadow-lg backdrop-blur supports-backdrop-filter:bg-muted/80">
        {MOBILE_NAV_ITEMS.map((item) => {
          const isActive = pathname === item.to
          return (
            <li key={item.to} className="flex-1">
              <Link
                to={item.to}
                aria-label={t(item.labelKey)}
                aria-current={isActive ? "page" : undefined}
                className={cn(
                  "flex h-11 w-full items-center justify-center rounded-full text-foreground/60 transition-all hover:text-foreground focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none",
                  isActive &&
                    "bg-background text-foreground shadow-sm dark:border dark:border-input dark:bg-input/30"
                )}
              >
                <item.icon className="size-5" />
              </Link>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
