import { useMutation, useQueryClient } from "@tanstack/react-query"
import { useNavigate } from "react-router-dom"

import { logout } from "@/features/auth/api/auth-api"
import { useSessionStore } from "@/features/auth/store/session-store"
import { ROUTES } from "@/lib/routes"

/**
 * Revoca el token en el backend y limpia la sesión local. La limpieza local
 * ocurre aunque el backend falle (sin red, token ya vencido): el usuario pidió
 * salir y no debe quedar dentro.
 */
export function useLogout() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const clearSession = useSessionStore((state) => state.clearSession)

  return useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearSession()
      queryClient.clear()
      void navigate(ROUTES.login, { replace: true })
    },
  })
}
