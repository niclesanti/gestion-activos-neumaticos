import { create } from "zustand"
import { createJSONStorage, persist } from "zustand/middleware"

import type { LoginResponse, SessionUser } from "@/features/auth/api/auth-api"

type SessionState = {
  token: string | null
  /** Instante ISO-8601 en que expira el token. */
  expiresAt: string | null
  user: SessionUser | null
  setSession: (response: LoginResponse) => void
  setUser: (user: SessionUser) => void
  clearSession: () => void
}

const EMPTY_SESSION = { token: null, expiresAt: null, user: null }

const SESSION_STORAGE_KEY = "session"

/**
 * Sesión del usuario. Se persiste en `sessionStorage` (no `localStorage`): una
 * recarga mantiene la sesión, pero cerrar la pestaña o el navegador la termina.
 */
export const useSessionStore = create<SessionState>()(
  persist(
    (set) => ({
      ...EMPTY_SESSION,
      setSession: (response) =>
        set({
          token: response.token,
          expiresAt: response.expiraEn,
          user: response.usuario,
        }),
      setUser: (user) => set({ user }),
      clearSession: () => set(EMPTY_SESSION),
    }),
    {
      name: SESSION_STORAGE_KEY,
      storage: createJSONStorage(() => sessionStorage),
      partialize: ({ token, expiresAt, user }) => ({ token, expiresAt, user }),
    }
  )
)

/**
 * Descarta lo que el store tenga en memoria y vuelve a leer `sessionStorage`,
 * la fuente que refleja logins y logouts hechos desde otro documento.
 */
export function reloadSessionFromStorage() {
  if (sessionStorage.getItem(SESSION_STORAGE_KEY) === null) {
    useSessionStore.getState().clearSession()
  } else {
    void useSessionStore.persist.rehydrate()
  }
}

/** Hay token y todavía no expiró. */
export function selectIsAuthenticated(state: SessionState) {
  return (
    state.token !== null &&
    state.expiresAt !== null &&
    Date.parse(state.expiresAt) > Date.now()
  )
}
