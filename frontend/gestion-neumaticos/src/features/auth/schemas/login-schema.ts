import { z } from "zod"

/** Mismo patrón que `IdentificadorValidator` en el backend. */
const USERNAME_PATTERN = /^[a-zA-Z0-9._-]{3,30}$/

/**
 * Los mensajes son claves del namespace `auth`, no texto: se traducen al
 * renderizar, así un error visible cambia de idioma junto con la interfaz.
 *
 * Reglas mínimas a propósito: en el login no se valida complejidad de la
 * contraseña (eso es del alta), y la contraseña no se recorta.
 */
export const loginSchema = z.object({
  identifier: z
    .string()
    .trim()
    .min(1, "login.validation.required")
    .max(254, "login.validation.identifierTooLong")
    .refine((value) => !/\s/.test(value), "login.validation.identifierSpaces")
    .refine(
      (value) =>
        value.includes("@")
          ? z.email().safeParse(value).success
          : USERNAME_PATTERN.test(value),
      "login.validation.identifierFormat"
    ),
  password: z
    .string()
    .min(1, "login.validation.required")
    .max(128, "login.validation.passwordTooLong"),
})

export type LoginInput = z.input<typeof loginSchema>
export type LoginValues = z.output<typeof loginSchema>
