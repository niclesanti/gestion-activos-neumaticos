import type { ParseKeys } from "i18next"
import { z } from "zod"

import { emptyToNull } from "@/lib/strings"

/*
 * Domicilio compartido por los features que lo piden (proveedores, servicios de
 * reparación…). Mismos límites y reglas que `DomicilioRequestDTO` en el
 * backend: si cambia uno, cambian ambos. Los mensajes son claves del namespace
 * `address`.
 */
const MAX_STREET_LENGTH = 100
const MAX_SHORT_LENGTH = 10
const MAX_CITY_LENGTH = 100
/** 4 dígitos o CPA (ej. S3000ABC), sin distinguir mayúsculas. */
const POSTAL_CODE_PATTERN = /^\s*([0-9]{4}|[A-Za-z][0-9]{4}[A-Za-z]{3})?\s*$/

/** Clave de un mensaje del namespace `address`. */
export type AddressMessageKey = ParseKeys<"address">

/** Domicilio (`DomicilioResponseDTO` del backend). */
export type Address = {
  calle: string
  altura: string
  piso: string | null
  departamento: string | null
  localidad: string
  codigoPostal: string | null
  provinciaId: number
  provincia: string
}

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

/** Campos del domicilio para incluir en el `z.object` del formulario. */
export const addressShape = {
  calle: z.string().max(MAX_STREET_LENGTH, "validation.maxLength100"),
  altura: z.string().max(MAX_SHORT_LENGTH, "validation.maxLength10"),
  piso: z.string().max(MAX_SHORT_LENGTH, "validation.maxLength10"),
  departamento: z.string().max(MAX_SHORT_LENGTH, "validation.maxLength10"),
  provinciaId: z.string(),
  localidad: z.string().max(MAX_CITY_LENGTH, "validation.maxLength100"),
  codigoPostal: z
    .string()
    .regex(POSTAL_CODE_PATTERN, "validation.postalCodeFormat"),
} satisfies Record<(typeof ADDRESS_FIELDS)[number], z.ZodString>

export type AddressFormInput = z.input<z.ZodObject<typeof addressShape>>

/** Hay domicilio si al menos un campo tiene algo más que espacios. */
export function hasAddress(address: Partial<AddressFormInput>) {
  return ADDRESS_FIELDS.some((field) => (address[field] ?? "").trim() !== "")
}

/**
 * El domicilio es opcional, pero si se empieza a cargar tiene que estar
 * completo. Se usa dentro del `superRefine` del formulario.
 */
export function validateAddress(
  address: AddressFormInput,
  ctx: z.RefinementCtx
) {
  if (!hasAddress(address)) return
  for (const field of REQUIRED_ADDRESS_FIELDS) {
    if (address[field].trim() === "") {
      ctx.addIssue({
        code: "custom",
        path: [field],
        message: "validation.addressRequired" satisfies AddressMessageKey,
      })
    }
  }
}

/** Cuerpo `domicilio` que espera la API, o `null` si no se cargó ninguno. */
export function toAddressRequest(address: AddressFormInput) {
  if (!hasAddress(address)) return null
  return {
    calle: address.calle.trim(),
    altura: address.altura.trim(),
    piso: emptyToNull(address.piso),
    departamento: emptyToNull(address.departamento),
    provinciaId: Number(address.provinciaId),
    localidad: address.localidad.trim(),
    codigoPostal: emptyToNull(address.codigoPostal),
  }
}
