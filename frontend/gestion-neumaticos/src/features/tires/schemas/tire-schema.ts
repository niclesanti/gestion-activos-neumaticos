import { z } from "zod"

export const TIRE_STATUSES = [
  "NUEVO",
  "NUEVO_USADO",
  "HOMBROS",
  "RECAPADO",
  "RECAPADO_USADO",
  "RECAPADO_SIN_GARANTIA",
  "DE_BAJA",
] as const
export type TireStatus = (typeof TIRE_STATUSES)[number]

/** Estados que se ofrecen al registrar: un neumático no se da de alta ya de baja. */
export const REGISTRABLE_TIRE_STATUSES = TIRE_STATUSES.filter(
  (status) => status !== "DE_BAJA"
)

export const AXLE_TYPES = [
  "DIRECCION",
  "TRACCION",
  "ARRASTRE",
  "AUXILIO",
  "SIN_ESPECIFICAR",
] as const
export type AxleType = (typeof AXLE_TYPES)[number]

/** Estados que exigen indicar al menos un recapado. */
export const RETREADED_STATUSES: readonly TireStatus[] = [
  "RECAPADO",
  "RECAPADO_USADO",
  "RECAPADO_SIN_GARANTIA",
]

/*
 * Mismos límites y reglas que `NeumaticoRequestDTO` en el backend: si cambia
 * uno, cambian ambos. Los mensajes son claves del namespace `tires`.
 */
const MAX_SERIAL_LENGTH = 100
/** Vacío o en blanco = sin número de serie; los extremos se recortan al enviar. */
const SERIAL_PATTERN = /^\s*([A-Za-z0-9][A-Za-z0-9 ./-]*)?\s*$/
const MAX_KM = 9_999_999
const MAX_RETREADS = 20
const MAX_COMMENT_LENGTH = 512

/*
 * Kilometrajes y recapados son opcionales (null = dato desconocido), salvo
 * donde el estado los exige: eso lo valida el superRefine.
 */
const km = z
  .number()
  .min(0, "validation.kmNegative")
  .max(MAX_KM, "validation.kmTooHigh")
  .nullable()

/** El valor si es un número válido; null si el campo está vacío o no lo es. */
function validNumber(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) ? value : null
}

/** Valor del input numérico para react-hook-form: vacío = null. */
export function toNullableNumber(value: unknown) {
  if (value === "" || value === null || value === undefined) return null
  return Number(value)
}

function emptyToNull(value: string) {
  const trimmed = value.trim()
  return trimmed === "" ? null : trimmed
}

export const tireSchema = z
  .object({
    numeroSerie: z
      .string()
      .max(MAX_SERIAL_LENGTH, "validation.serialTooLong")
      .regex(SERIAL_PATTERN, "validation.serialFormat"),
    estadoActual: z.enum(TIRE_STATUSES, { error: "validation.required" }),
    marcaId: z.string().min(1, "validation.required"),
    proveedorId: z.string(),
    tipoOrigen: z.enum(AXLE_TYPES, { error: "validation.required" }),
    tipoActual: z.enum(AXLE_TYPES, { error: "validation.required" }),
    kilometrajeTotal: km,
    kilometrajeUltimaReparacion: km,
    cantidadRecapados: z
      .number()
      .int("validation.retreadsInteger")
      .min(0, "validation.retreadsNegative")
      .max(MAX_RETREADS, "validation.retreadsTooHigh")
      .nullable(),
    comentario: z.string().max(MAX_COMMENT_LENGTH, "validation.commentTooLong"),
  })
  .superRefine(
    (tire, ctx) => {
      const issue = (path: keyof typeof tire, message: string) =>
        ctx.addIssue({ code: "custom", path: [path], message })
      // Corre aunque otros campos sean inválidos (ver `when`): cada regla
      // solo mira los datos que ya son válidos, para no duplicar errores.
      const total = validNumber(tire.kilometrajeTotal)
      const sinceRepair = validNumber(tire.kilometrajeUltimaReparacion)
      const retreads = validNumber(tire.cantidadRecapados)

      if (tire.estadoActual === "DE_BAJA") {
        issue("estadoActual", "validation.statusNotAllowed")
      }
      if (total !== null && sinceRepair !== null && sinceRepair > total) {
        issue(
          "kilometrajeUltimaReparacion",
          "validation.kmSinceRepairAboveTotal"
        )
      }
      // Un neumático nuevo no tiene uso: los tres datos son obligatorios y en 0.
      if (tire.estadoActual === "NUEVO") {
        if (tire.kilometrajeTotal === null) {
          issue("kilometrajeTotal", "validation.required")
        } else if (total !== null && total !== 0) {
          issue("kilometrajeTotal", "validation.newTireKm")
        }
        if (tire.kilometrajeUltimaReparacion === null) {
          issue("kilometrajeUltimaReparacion", "validation.required")
        } else if (sinceRepair !== null && sinceRepair !== 0) {
          issue("kilometrajeUltimaReparacion", "validation.newTireKm")
        }
        if (tire.cantidadRecapados === null) {
          issue("cantidadRecapados", "validation.required")
        } else if (retreads !== null && retreads !== 0) {
          issue("cantidadRecapados", "validation.newTireRetreads")
        }
      }
      if (RETREADED_STATUSES.includes(tire.estadoActual)) {
        if (tire.cantidadRecapados === null) {
          issue("cantidadRecapados", "validation.required")
        } else if (retreads !== null && retreads < 1) {
          issue("cantidadRecapados", "validation.retreadedNeedsRetread")
        }
      }
    },
    // Por defecto zod omite las reglas entre campos mientras haya otros
    // errores; así se muestran todos los problemas en el primer envío.
    { when: () => true }
  )
  // Cuerpo exacto que espera `POST /api/v1/neumaticos`.
  .transform((tire) => ({
    // Normalizado como en el backend: recortado y en mayúsculas.
    numeroSerie: emptyToNull(tire.numeroSerie)?.toUpperCase() ?? null,
    estadoActual: tire.estadoActual,
    kilometrajeTotal: tire.kilometrajeTotal,
    kilometrajeUltimaReparacion: tire.kilometrajeUltimaReparacion,
    tipoOrigen: tire.tipoOrigen,
    tipoActual: tire.tipoActual,
    cantidadRecapados: tire.cantidadRecapados,
    comentario: emptyToNull(tire.comentario),
    marcaId: Number(tire.marcaId),
    // El proveedor solo corresponde a la compra de un neumático nuevo.
    proveedorId:
      tire.estadoActual === "NUEVO" && tire.proveedorId !== ""
        ? tire.proveedorId
        : null,
  }))

export type TireFormInput = z.input<typeof tireSchema>
export type TireFormValues = z.output<typeof tireSchema>
