import * as React from "react"

import type { AccessLevel } from "@/lib/access"

export type SessionUser = {
  name: string
  accessLevel: AccessLevel
}

/**
 * Usuario de la sesión para los features, que no pueden importar
 * `features/auth`: lo provee el shell (`AppLayout`), que sí lee la sesión.
 */
export const SessionUserContext = React.createContext<SessionUser | null>(null)

export function useSessionUser() {
  return React.useContext(SessionUserContext)
}
