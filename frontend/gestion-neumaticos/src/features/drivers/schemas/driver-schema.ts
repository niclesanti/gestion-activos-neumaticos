import { z } from "zod"

/*
 * Mismos límites y reglas que `ConductorRequestDTO` en el backend: si cambia
 * uno, cambian ambos. Los mensajes son claves del namespace `drivers`.
 */
const MAX_NAME_LENGTH = 100
/** Vacío o en blanco = sin DNI; los extremos se recortan al enviar. */
const DNI_PATTERN = /^\s*([0-9]{7,8})?\s*$/

function emptyToNull(value: string) {
  const trimmed = value.trim()
  return trimmed === "" ? null : trimmed
}

/** Obligatorio (no en blanco) y con el largo máximo del backend. */
const requiredName = z
  .string()
  .max(MAX_NAME_LENGTH, "validation.nameTooLong")
  .refine((value) => value.trim() !== "", "validation.required")

export const driverSchema = z
  .object({
    dni: z.string().regex(DNI_PATTERN, "validation.dniFormat"),
    nombre: requiredName,
    apellido: requiredName,
  })
  // Cuerpo exacto que espera `POST /api/v1/conductores`.
  .transform((driver) => ({
    dni: emptyToNull(driver.dni),
    nombre: driver.nombre.trim(),
    apellido: driver.apellido.trim(),
  }))

export type DriverFormInput = z.input<typeof driverSchema>
export type DriverFormValues = z.output<typeof driverSchema>
