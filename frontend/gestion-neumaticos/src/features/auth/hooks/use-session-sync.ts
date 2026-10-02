import * as React from "react"

import { useQuery } from "@tanstack/react-query"

import { getMe } from "@/features/auth/api/auth-api"
import {
  selectIsAuthenticated,
  useSessionStore,
} from "@/features/auth/store/session-store"

/**
 * Al restaurar una sesión guardada, la valida contra el backend y refresca los
 * datos del usuario (su nivel de acceso pudo cambiar). Si el token ya no sirve,
 * el interceptor de axios ve el 401 y cierra la sesión.
 */
export function useSessionSync() {
  const isAuthenticated = useSessionStore(selectIsAuthenticated)
  const publicId = useSessionStore((state) => state.user?.publicId)
  const setUser = useSessionStore((state) => state.setUser)

  const { data } = useQuery({
    // La clave incluye al usuario: una respuesta cacheada de otra sesión nunca
    // puede pisar al usuario actual.
    queryKey: ["auth", "me", publicId],
    queryFn: getMe,
    enabled: isAuthenticated && publicId !== undefined,
    staleTime: Infinity,
  })

  // La respuesta del backend corresponde al token: es la fuente de verdad.
  React.useEffect(() => {
    if (data) {
      setUser(data)
    }
  }, [data, setUser])
}
