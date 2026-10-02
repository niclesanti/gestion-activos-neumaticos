import { Navigate, Outlet, useLocation } from "react-router-dom"

import { useSessionSync } from "@/features/auth/hooks/use-session-sync"
import {
  selectIsAuthenticated,
  useSessionStore,
} from "@/features/auth/store/session-store"
import { ROUTES } from "@/lib/routes"

/** Estado de navegación que usa el login para volver a donde estaba el usuario. */
export type LoginLocationState = { from?: string }

/**
 * Rutas protegidas: sin sesión válida se redirige al login, recordando la
 * pantalla pedida para volver a ella después de ingresar.
 */
export function RequireAuth() {
  const isAuthenticated = useSessionStore(selectIsAuthenticated)
  const location = useLocation()
  useSessionSync()

  if (!isAuthenticated) {
    const state: LoginLocationState = {
      from: location.pathname + location.search,
    }
    return <Navigate to={ROUTES.login} replace state={state} />
  }

  return <Outlet />
}
