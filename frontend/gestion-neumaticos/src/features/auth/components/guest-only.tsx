import { Navigate, Outlet, useLocation } from "react-router-dom"

import type { LoginLocationState } from "@/features/auth/components/require-auth"
import {
  selectIsAuthenticated,
  useSessionStore,
} from "@/features/auth/store/session-store"
import { ROUTES } from "@/lib/routes"

/**
 * Pantallas sólo para usuarios sin sesión (el login). Al haber sesión —
 * incluido el instante en que el login la guarda— lleva a la pantalla que el
 * usuario había pedido antes de ingresar, o al tablero de control.
 */
export function GuestOnly() {
  const isAuthenticated = useSessionStore(selectIsAuthenticated)
  const location = useLocation()

  if (isAuthenticated) {
    const from = (location.state as LoginLocationState | null)?.from
    return <Navigate to={from ?? ROUTES.home} replace />
  }

  return <Outlet />
}
