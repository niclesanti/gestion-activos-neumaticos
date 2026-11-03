import { z } from "zod"

import {
  addressShape,
  toRequiredAddressRequest,
  validateRequiredAddress,
} from "@/lib/address"

/*
 * Mismos límites y reglas que `CentroAlmacenamientoRequestDTO` en el backend:
 * si cambia uno, cambian ambos. Los mensajes son claves del namespace
 * `storage-centers`; los del domicilio (compartido) son del namespace `address`.
 * A diferencia de proveedores y servicios de reparación, el domicilio es
 * obligatorio.
 */
const MAX_NAME_LENGTH = 150

export const storageCenterSchema = z
  .object({
    nombre: z
      .string()
      .max(MAX_NAME_LENGTH, "validation.maxLength150")
      .refine((value) => value.trim() !== "", "validation.required"),
    ...addressShape,
  })
  .superRefine(validateRequiredAddress)
  // Cuerpo exacto que espera `POST /api/v1/centros-almacenamiento`; el stock
  // no se envía: el backend siempre lo inicializa en 0.
  .transform((center) => ({
    nombre: center.nombre.trim(),
    domicilio: toRequiredAddressRequest(center),
  }))

export type StorageCenterFormInput = z.input<typeof storageCenterSchema>
export type StorageCenterFormValues = z.output<typeof storageCenterSchema>
