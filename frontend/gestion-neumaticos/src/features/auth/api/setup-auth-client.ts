import {
  reloadSessionFromStorage,
  useSessionStore,
} from "@/features/auth/store/session-store"
import { configureApiClient } from "@/lib/api/client"
import { queryClient } from "@/lib/api/query-client"
import { notify } from "@/lib/toast"

/**
 * Conecta el cliente HTTP con la sesión: agrega el Bearer a cada request y, si
 * el backend rechaza el token (expirado o revocado), cierra la sesión local.
 * Los guards de ruta reaccionan al store y llevan al login.
 */
export function setupAuthClient() {
  return configureApiClient({
    getToken: () => useSessionStore.getState().token,
    onUnauthorized: () => {
      if (useSessionStore.getState().token === null) {
        return
      }
      useSessionStore.getState().clearSession()
      queryClient.clear()
      notify.warning({
        titleKey: "auth:session.expired",
        id: "session-expired",
      })
    },
  })
}

/**
 * El botón "atrás" del navegador puede restaurar un documento anterior desde
 * el back/forward cache, con la memoria de JS tal como estaba: el store y la
 * caché de react-query seguirían teniendo la sesión y los datos del usuario
 * aunque después se haya cerrado sesión (o ingresado otro usuario). Al
 * restaurarse la página se descarta esa memoria y se relee `sessionStorage`.
 */
export function syncSessionOnPageRestore() {
  const onPageShow = (event: PageTransitionEvent) => {
    if (!event.persisted) {
      return
    }
    queryClient.clear()
    reloadSessionFromStorage()
  }

  window.addEventListener("pageshow", onPageShow)
  return () => window.removeEventListener("pageshow", onPageShow)
}
