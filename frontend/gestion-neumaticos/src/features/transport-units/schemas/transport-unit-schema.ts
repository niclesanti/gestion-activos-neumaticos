import { z } from "zod"

/*
 * Mismas reglas que `UnidadTransporteRequestDTO` y `Patentes` en el backend (y
 * el CHECK de la migración V16): si cambia uno, cambian todos. Los mensajes son
 * claves del namespace `transport-units`.
 */
const MAX_PLATE_LENGTH = 15
/** Formato anterior (ABC123) o Mercosur (AB123CD), ya normalizada. */
const PLATE_PATTERN = /^([A-Z]{3}[0-9]{3}|[A-Z]{2}[0-9]{3}[A-Z]{2})$/

/** Mayúsculas, sin espacios ni guiones: "ab 123-cd" → "AB123CD". */
export function normalizePlate(value: string) {
  return value.replace(/[\s-]/g, "").toUpperCase()
}

export const transportUnitSchema = z
  .object({
    patente: z
      .string()
      .max(MAX_PLATE_LENGTH, "validation.maxLength15")
      .refine((value) => value.trim() !== "", "validation.required")
      .refine(
        (value) =>
          value.trim() === "" || PLATE_PATTERN.test(normalizePlate(value)),
        "validation.plateFormat"
      ),
    configuracionId: z.string().min(1, "validation.configurationRequired"),
  })
  // Cuerpo exacto que espera `POST /api/v1/unidades-transporte`.
  .transform((unit) => ({
    patente: normalizePlate(unit.patente),
    configuracionId: Number(unit.configuracionId),
  }))

export type TransportUnitFormInput = z.input<typeof transportUnitSchema>
export type TransportUnitFormValues = z.output<typeof transportUnitSchema>
