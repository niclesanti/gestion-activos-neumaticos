import { z } from "zod"

import { addressShape, toAddressRequest, validateAddress } from "@/lib/address"
import { emptyToNull } from "@/lib/strings"

/*
 * Mismos límites y reglas que `ProveedorRequestDTO` en el backend: si cambia
 * uno, cambian ambos. Los mensajes son claves del namespace `suppliers`; los
 * del domicilio (compartido) son del namespace `address`.
 */
const MAX_NAME_LENGTH = 150
const MAX_EMAIL_LENGTH = 254
/** Vacío o en blanco = sin email; los extremos se recortan al enviar. */
const EMAIL_PATTERN = /^\s*([^@\s]+@[^@\s]+\.[^@\s]+)?\s*$/
/** Dígitos, espacios, +, -, ( y ), entre 6 y 30 caracteres. */
const PHONE_PATTERN = /^\s*(\+?[0-9(][0-9 ()-]{4,27}[0-9])?\s*$/

export const supplierSchema = z
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
  // Cuerpo exacto que espera `POST /api/v1/proveedores`.
  .transform((supplier) => ({
    nombre: supplier.nombre.trim(),
    email: emptyToNull(supplier.email),
    telefono: emptyToNull(supplier.telefono),
    domicilio: toAddressRequest(supplier),
  }))

export type SupplierFormInput = z.input<typeof supplierSchema>
export type SupplierFormValues = z.output<typeof supplierSchema>
