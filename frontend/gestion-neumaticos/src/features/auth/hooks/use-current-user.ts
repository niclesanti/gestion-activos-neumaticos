import { useSessionStore } from "@/features/auth/store/session-store"
import type { AccessLevel } from "@/lib/access"

export type CurrentUser = {
  name: string
  email: string
  avatarUrl: string
  initials: string
  accessLevel: AccessLevel
}

/** Primeras letras de las dos primeras palabras del nombre, para el AvatarFallback. */
function getInitials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0]?.toUpperCase() ?? "")
    .join("")
}

/** Usuario de la sesión actual, o `null` si no hay sesión. */
export function useCurrentUser(): CurrentUser | null {
  const user = useSessionStore((state) => state.user)

  if (!user) {
    return null
  }

  return {
    name: user.nombreApellido,
    email: user.email,
    // El backend todavía no maneja avatares: se muestran las iniciales.
    avatarUrl: "",
    initials: getInitials(user.nombreApellido),
    accessLevel: user.nivelAcceso,
  }
}
