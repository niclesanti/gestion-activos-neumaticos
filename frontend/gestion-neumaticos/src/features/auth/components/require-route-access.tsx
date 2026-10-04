import { Navigate, Outlet, useLocation } from "react-router-dom"

import { useCurrentUser } from "@/features/auth/hooks/use-current-user"
import { canAccessRoute } from "@/lib/access"
import { ROUTES } from "@/lib/routes"

/**
 * Bloquea por URL las secciones que el nivel de acceso del usuario no tiene
 * (las reglas viven en `ROUTE_ACCESS`): que un ítem no figure en la sidebar no
 * impide tipear la ruta a mano.
 */
export function RequireRouteAccess() {
  const user = useCurrentUser()
  const { pathname } = useLocation()

  if (!canAccessRoute(user?.accessLevel, pathname)) {
    return <Navigate to={ROUTES.home} replace />
  }

  return <Outlet />
}
