import { z } from "zod"

/**
 * Variables de entorno validadas al arrancar: una URL mal configurada falla
 * acá, con un mensaje claro, y no como un error de red difuso en el login.
 * Vite las incrusta en el bundle en tiempo de build.
 */
const envSchema = z.object({
  VITE_API_URL: z.url(),
})

const parsed = envSchema.safeParse(import.meta.env)

if (!parsed.success) {
  throw new Error(
    `Variables de entorno inválidas: ${z.prettifyError(parsed.error)}`
  )
}

export const env = {
  /** URL base del backend, sin barra final. */
  apiUrl: parsed.data.VITE_API_URL.replace(/\/+$/, ""),
}
