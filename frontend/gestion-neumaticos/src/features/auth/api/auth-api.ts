import type { LoginValues } from "@/features/auth/schemas/login-schema"
import type { AccessLevel } from "@/lib/access"
import { apiClient } from "@/lib/api/client"

/** Usuario autenticado (`UsuarioSesionDTO` del backend). */
export type SessionUser = {
  publicId: string
  nombreUsuario: string
  nombreApellido: string
  email: string
  nivelAcceso: AccessLevel
}

/** Respuesta del login (`LoginResponseDTO` del backend). */
export type LoginResponse = {
  token: string
  tipo: "Bearer"
  /** Instante ISO-8601 (UTC) en que expira el token. */
  expiraEn: string
  usuario: SessionUser
}

export async function login(values: LoginValues) {
  const { data } = await apiClient.post<LoginResponse>(
    "/api/auth/login",
    values
  )
  return data
}

/**
 * Revoca el token en el backend. Por defecto usa el de la sesión actual; se
 * puede pasar uno explícito para revocarlo aunque la sesión local ya se haya
 * limpiado (cierre por inactividad).
 */
export async function logout(token?: string) {
  await apiClient.post(
    "/api/auth/logout",
    null,
    token ? { headers: { Authorization: `Bearer ${token}` } } : undefined
  )
}

export async function getMe() {
  const { data } = await apiClient.get<SessionUser>("/api/auth/me")
  return data
}
