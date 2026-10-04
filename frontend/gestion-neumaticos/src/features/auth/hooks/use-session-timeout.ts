import * as React from "react"

import { useQueryClient } from "@tanstack/react-query"

import { logout } from "@/features/auth/api/auth-api"
import { useSessionStore } from "@/features/auth/store/session-store"
import { env } from "@/lib/env"
import { notify } from "@/lib/toast"

/** Interacciones que cuentan como actividad del usuario. */
const ACTIVITY_EVENTS = ["pointerdown", "keydown", "wheel", "touchstart"]

/** Cada cuánto se revisa si la sesión venció o quedó inactiva. */
const CHECK_INTERVAL_MS = 15_000

/** Prefijo de la clave de `localStorage` con la última actividad de la sesión. */
const ACTIVITY_KEY_PREFIX = "session-activity:"

/** Como mucho una escritura en `localStorage` cada 5 segundos. */
const SHARE_THROTTLE_MS = 5_000

/** `localStorage` puede no estar disponible (modo privado, bloqueado): se ignora. */
function readSharedActivity(key: string) {
  try {
    return Number(localStorage.getItem(key)) || 0
  } catch {
    return 0
  }
}

function writeSharedActivity(key: string, timestamp: number) {
  try {
    // Solo queda la marca de la sesión actual.
    for (const existing of Object.keys(localStorage)) {
      if (existing.startsWith(ACTIVITY_KEY_PREFIX) && existing !== key) {
        localStorage.removeItem(existing)
      }
    }
    localStorage.setItem(key, String(timestamp))
  } catch {
    // Sin localStorage cada pestaña mide su propia actividad.
  }
}

/**
 * Cierra la sesión sola en dos casos:
 * - **Inactividad** (`VITE_SESSION_IDLE_MINUTES`, 30 por defecto): además de
 *   limpiar la sesión local revoca el token en el backend, así una pestaña
 *   olvidada abierta no deja un token válido hasta su expiración.
 * - **Expiración del token** (`expiresAt`): el backend ya lo rechaza; se limpia
 *   la sesión sin esperar a que un request devuelva 401.
 *
 * Al volver a una pestaña en segundo plano se revisa de inmediato: los
 * navegadores frenan los timers de las pestañas ocultas.
 */
export function useSessionTimeout() {
  const token = useSessionStore((state) => state.token)
  const expiresAt = useSessionStore((state) => state.expiresAt)
  const clearSession = useSessionStore((state) => state.clearSession)
  const queryClient = useQueryClient()

  React.useEffect(() => {
    if (token === null || expiresAt === null) {
      return
    }

    // Una pestaña duplicada copia el sessionStorage y comparte el token: la
    // actividad se publica en localStorage para que una pestaña quieta no
    // revoque el token que el usuario está usando en otra. La clave es la
    // expiración de la sesión (no el token, que nunca va a localStorage).
    const activityKey = `${ACTIVITY_KEY_PREFIX}${expiresAt}`
    let lastActivity = Date.now()
    let lastShared = 0
    const markActivity = () => {
      lastActivity = Date.now()
      if (lastActivity - lastShared >= SHARE_THROTTLE_MS) {
        lastShared = lastActivity
        writeSharedActivity(activityKey, lastActivity)
      }
    }
    markActivity()

    const endSession = (reason: "idle" | "expired") => {
      // Otro login o un logout ya cambiaron la sesión: no tocar la nueva.
      if (useSessionStore.getState().token !== token) {
        return
      }
      if (reason === "idle") {
        // Se pasa el token explícito porque la sesión local se limpia antes de
        // que salga el request. Si falla (sin red), el token vence igual.
        logout(token).catch(() => undefined)
      }
      clearSession()
      queryClient.clear()
      notify.warning({
        titleKey:
          reason === "idle" ? "auth:session.idle" : "auth:session.expired",
        id: "session-expired",
      })
    }

    const check = () => {
      const now = Date.now()
      if (now >= Date.parse(expiresAt)) {
        endSession("expired")
      } else if (
        now - Math.max(lastActivity, readSharedActivity(activityKey)) >=
        env.sessionIdleMs
      ) {
        endSession("idle")
      }
    }

    const onVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        check()
      }
    }

    for (const event of ACTIVITY_EVENTS) {
      window.addEventListener(event, markActivity, { passive: true })
    }
    document.addEventListener("visibilitychange", onVisibilityChange)
    const interval = window.setInterval(check, CHECK_INTERVAL_MS)

    return () => {
      for (const event of ACTIVITY_EVENTS) {
        window.removeEventListener(event, markActivity)
      }
      document.removeEventListener("visibilitychange", onVisibilityChange)
      window.clearInterval(interval)
    }
  }, [token, expiresAt, clearSession, queryClient])
}
