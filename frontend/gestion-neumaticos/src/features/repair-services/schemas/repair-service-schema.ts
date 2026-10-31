import { z } from "zod"

import { addressShape, toAddressRequest, validateAddress } from "@/lib/address"
import { emptyToNull } from "@/lib/strings"

/*
 * Mismos límites y reglas que `ServicioReparacionRequestDTO` en el backend: si
 * cambia uno, cambian ambos. Los mensajes son claves del namespace
 * `repair-services`; los del domicilio (compartido) son del namespace `address`.
 */
const MAX_NAME_LENGTH = 150
const MAX_EMAIL_LENGTH = 254
/** Vacío o en blanco = sin email; los extremos se recortan al enviar. */
const EMAIL_PATTERN = /^\s*([^@\s]+@[^@\s]+\.[^@\s]+)?\s*$/
/** Dígitos, espacios, +, -, ( y ), entre 6 y 30 caracteres. */
const PHONE_PATTERN = /^\s*(\+?[0-9(][0-9 ()-]{4,27}[0-9])?\s*$/

export const repairServiceSchema = z
  .object({
    nombre: z
      .string()
      .max(MAX_NAME_LENGTH, "validation.maxLength150")
      .refine((value) => value.trim() !== "", "validation.required"),
    email: z
      .string()
      .max(MAX_EMAIL_LENGTH, "validation.maxLength254")
      .regex(EMAIL_PATTERN, "validation.emailFormat"),
    telefono: z.string().regex(PHONE_PATTERN, "validation.phoneFormat"),
    ...addressShape,
  })
  .superRefine(validateAddress)
  // Cuerpo exacto que espera `POST /api/v1/servicios-reparacion`.
  .transform((service) => ({
    nombre: service.nombre.trim(),
    email: emptyToNull(service.email),
    telefono: emptyToNull(service.telefono),
    domicilio: toAddressRequest(service),
  }))

export type RepairServiceFormInput = z.input<typeof repairServiceSchema>
export type RepairServiceFormValues = z.output<typeof repairServiceSchema>
