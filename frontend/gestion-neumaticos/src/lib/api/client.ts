import axios, { type InternalAxiosRequestConfig } from "axios"

import { env } from "@/lib/env"

/** Instancia única de axios contra el backend. */
export const apiClient = axios.create({
  baseURL: env.apiUrl,
  headers: { "Content-Type": "application/json" },
  timeout: 15_000,
})

type ApiClientAuth = {
  /** Token de la sesión actual, o `null` si no hay sesión. */
  getToken: () => string | null
  /** Se invoca cuando el backend rechaza el token (expirado o revocado). */
  onUnauthorized: () => void
}

/** Requests que pueden devolver 401 sin que eso signifique "sesión vencida". */
const AUTH_ENDPOINTS_WITHOUT_SESSION = ["/api/auth/login"]

/**
 * `src/lib` no puede importar features: el feature de autenticación registra
 * acá cómo obtener el token y qué hacer ante un 401, desde `src/app/providers`.
 */
export function configureApiClient({
  getToken,
  onUnauthorized,
}: ApiClientAuth) {
  const requestInterceptor = apiClient.interceptors.request.use((config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  const responseInterceptor = apiClient.interceptors.response.use(
    (response) => response,
    (error: unknown) => {
      if (
        axios.isAxiosError(error) &&
        error.response?.status === 401 &&
        !AUTH_ENDPOINTS_WITHOUT_SESSION.includes(error.config?.url ?? "") &&
        // Un 401 tardío de un request hecho con un token anterior (por ejemplo,
        // el de la sesión previa a un nuevo login) no debe cerrar la sesión actual.
        sentToken(error.config) === getToken()
      ) {
        onUnauthorized()
      }
      return Promise.reject(error)
    }
  )

  return () => {
    apiClient.interceptors.request.eject(requestInterceptor)
    apiClient.interceptors.response.eject(responseInterceptor)
  }
}

/** Token Bearer con el que salió un request, o `null` si salió sin token. */
function sentToken(config: InternalAxiosRequestConfig | undefined) {
  const header = config?.headers?.Authorization
  return typeof header === "string" && header.startsWith("Bearer ")
    ? header.slice("Bearer ".length)
    : null
}

/** Código HTTP de un error de axios, o `undefined` si no hubo respuesta. */
export function getHttpStatus(error: unknown) {
  return axios.isAxiosError(error) ? error.response?.status : undefined
}

/**
 * Segundos del header `Retry-After` de un 429/503, o `undefined` si no vino o
 * no es un número de segundos.
 */
export function getRetryAfterSeconds(error: unknown) {
  if (!axios.isAxiosError(error)) {
    return undefined
  }
  const seconds = Number(error.response?.headers["retry-after"])
  return Number.isFinite(seconds) && seconds > 0 ? seconds : undefined
}
