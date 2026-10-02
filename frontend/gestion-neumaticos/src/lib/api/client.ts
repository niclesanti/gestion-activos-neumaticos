import axios from "axios"

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
        !AUTH_ENDPOINTS_WITHOUT_SESSION.includes(error.config?.url ?? "")
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

/** Código HTTP de un error de axios, o `undefined` si no hubo respuesta. */
export function getHttpStatus(error: unknown) {
  return axios.isAxiosError(error) ? error.response?.status : undefined
}
