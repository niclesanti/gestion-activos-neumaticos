import * as React from "react"
import { useTranslation } from "react-i18next"
import { Link, useLocation } from "react-router-dom"

import {
  Breadcrumb,
  BreadcrumbEllipsis,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { HOME_PATH, ROUTE_LABELS } from "@/lib/navigation"

type Crumb = { to: string; label: string }

/** Cantidad máxima de tramos visibles antes de colapsar los del medio. */
const MAX_VISIBLE = 3

function toTitleCase(segment: string) {
  const words = segment.replace(/-/g, " ")
  return words.charAt(0).toUpperCase() + words.slice(1)
}

function NavBreadcrumb() {
  const { t } = useTranslation()
  const { pathname } = useLocation()

  const segments = pathname.split("/").filter(Boolean)

  const crumbs: Crumb[] = segments.map((segment, index) => {
    const to = `/${segments.slice(0, index + 1).join("/")}`
    const labelKey = ROUTE_LABELS[to]
    // Los tramos sin ruta registrada (ids, subrutas futuras) caen a title case.
    return { to, label: labelKey ? t(labelKey) : toTitleCase(segment) }
  })

  // Toda ruta cuelga de Inicio, salvo la propia /home que ya es el primer tramo.
  if (pathname !== HOME_PATH) {
    crumbs.unshift({ to: HOME_PATH, label: t(ROUTE_LABELS[HOME_PATH]) })
  }

  if (crumbs.length === 0) {
    return null
  }

  // Con más de MAX_VISIBLE tramos se muestran el primero y los últimos, y el
  // resto se colapsa en una elipsis.
  const collapsed = crumbs.length > MAX_VISIBLE
  const visible = collapsed
    ? [crumbs[0], ...crumbs.slice(-(MAX_VISIBLE - 1))]
    : crumbs

  return (
    <Breadcrumb aria-label={t("breadcrumb.label")}>
      <BreadcrumbList>
        {visible.map((crumb, index) => {
          const isLast = index === visible.length - 1
          return (
            <React.Fragment key={crumb.to}>
              <BreadcrumbItem>
                {isLast ? (
                  <BreadcrumbPage>{crumb.label}</BreadcrumbPage>
                ) : (
                  <BreadcrumbLink render={<Link to={crumb.to} />}>
                    {crumb.label}
                  </BreadcrumbLink>
                )}
              </BreadcrumbItem>
              {isLast ? null : <BreadcrumbSeparator />}
              {collapsed && index === 0 ? (
                <>
                  <BreadcrumbItem>
                    <BreadcrumbEllipsis />
                  </BreadcrumbItem>
                  <BreadcrumbSeparator />
                </>
              ) : null}
            </React.Fragment>
          )
        })}
      </BreadcrumbList>
    </Breadcrumb>
  )
}

export { NavBreadcrumb }
