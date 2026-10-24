import { z } from "zod"

/*
 * Mismos límites y reglas que `ProveedorRequestDTO` y `DomicilioRequestDTO` en
 * el backend: si cambia uno, cambian ambos. Los mensajes son claves del
 * namespace `suppliers`.
 */
const MAX_NAME_LENGTH = 150
const MAX_EMAIL_LENGTH = 254
const MAX_STREET_LENGTH = 100
const MAX_SHORT_LENGTH = 10
const MAX_CITY_LENGTH = 100
/** Vacío o en blanco = sin email; los extremos se recortan al enviar. */
const EMAIL_PATTERN = /^\s*([^@\s]+@[^@\s]+\.[^@\s]+)?\s*$/
/** Dígitos, espacios, +, -, ( y ), entre 6 y 30 caracteres. */
const PHONE_PATTERN = /^\s*(\+?[0-9(][0-9 ()-]{4,27}[0-9])?\s*$/
/** 4 dígitos o CPA (ej. S3000ABC), sin distinguir mayúsculas. */
const POSTAL_CODE_PATTERN = /^\s*([0-9]{4}|[A-Za-z][0-9]{4}[A-Za-z]{3})?\s*$/

/** Campos del domicilio: si se completa alguno, se envía el domicilio. */
export const ADDRESS_FIELDS = [
  "calle",
  "altura",
  "piso",
  "departamento",
  "provinciaId",
  "localidad",
  "codigoPostal",
] as const

/** Obligatorios del domicilio, solo cuando se está cargando uno. */
const REQUIRED_ADDRESS_FIELDS = [
  "calle",
  "altura",
  "provinciaId",
  "localidad",
] as const

function emptyToNull(value: string) {
  const trimmed = value.trim()
  return trimmed === "" ? null : trimmed
}

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
    calle: z.string().max(MAX_STREET_LENGTH, "validation.maxLength100"),
    altura: z.string().max(MAX_SHORT_LENGTH, "validation.maxLength10"),
    piso: z.string().max(MAX_SHORT_LENGTH, "validation.maxLength10"),
    departamento: z.string().max(MAX_SHORT_LENGTH, "validation.maxLength10"),
    provinciaId: z.string(),
    localidad: z.string().max(MAX_CITY_LENGTH, "validation.maxLength100"),
    codigoPostal: z
      .string()
      .regex(POSTAL_CODE_PATTERN, "validation.postalCodeFormat"),
  })
  // El domicilio es opcional, pero si se empieza a cargar tiene que estar completo.
  .superRefine((supplier, ctx) => {
    if (!hasAddress(supplier)) return
    for (const field of REQUIRED_ADDRESS_FIELDS) {
      if (supplier[field].trim() === "") {
        ctx.addIssue({
          code: "custom",
          path: [field],
          message: "validation.addressRequired",
        })
      }
    }
  })
  // Cuerpo exacto que espera `POST /api/proveedores`.
  .transform((supplier) => ({
    nombre: supplier.nombre.trim(),
    email: emptyToNull(supplier.email),
    telefono: emptyToNull(supplier.telefono),
    domicilio: hasAddress(supplier)
      ? {
          calle: supplier.calle.trim(),
          altura: supplier.altura.trim(),
          piso: emptyToNull(supplier.piso),
          departamento: emptyToNull(supplier.departamento),
          provinciaId: Number(supplier.provinciaId),
          localidad: supplier.localidad.trim(),
          codigoPostal: emptyToNull(supplier.codigoPostal),
        }
      : null,
  }))

export type SupplierFormInput = z.input<typeof supplierSchema>
export type SupplierFormValues = z.output<typeof supplierSchema>

/** Hay domicilio si al menos un campo tiene algo más que espacios. */
export function hasAddress(
  supplier: Record<(typeof ADDRESS_FIELDS)[number], string>
) {
  return ADDRESS_FIELDS.some((field) => supplier[field].trim() !== "")
}
