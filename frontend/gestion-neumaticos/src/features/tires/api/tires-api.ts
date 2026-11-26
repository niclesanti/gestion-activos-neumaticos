import type {
  AxleType,
  TireFormValues,
  TireStatus,
} from "@/features/tires/schemas/tire-schema"
import { apiClient } from "@/lib/api/client"
import { DEFAULT_PAGE_SIZE, type Page } from "@/lib/api/page"

/** Marca de neumático (`MarcaNeumaticoResponseDTO` del backend). */
export type TireBrand = {
  id: number
  nombre: string
}

/** Neumático (`NeumaticoResponseDTO` del backend). */
export type Tire = {
  publicId: string
  numeroSerie: string | null
  estadoActual: TireStatus
  enEspera: boolean
  kilometrajeTotal: number | null
  kilometrajeUltimaReparacion: number | null
  tipoOrigen: AxleType
  tipoActual: AxleType
  cantidadRecapados: number | null
  situacionOperativaVigente: string
  comentario: string | null
  marcaNombre: string
}

export const OPERATIONAL_SITUATIONS = [
  "EN_STOCK",
  "MONTADO",
  "EN_REPARACION",
  "EN_ESPERA",
  "DE_BAJA",
] as const
export type OperationalSituation = (typeof OPERATIONAL_SITUATIONS)[number]

/** Fila del listado (`NeumaticoListadoDTO` del backend). */
export type TireListItem = {
  publicId: string
  numeroSerie: string | null
  marca: string
  estadoActual: TireStatus
  situacionOperativaVigente: OperationalSituation
  kilometrajeTotal: number | null
  /** Nombre del centro si está en stock; null si no. */
  centroAlmacenamiento: string | null
}

export type TiresQuery = {
  /** Texto de búsqueda; vacío = sin filtro. */
  numeroSerie: string
  /** Id de la marca (de `/marcas`); null = todas. */
  marcaId: number | null
  /** Estado técnico; null = todos. */
  estado: TireStatus | null
  /** Situación operativa; null = todas. */
  situacion: OperationalSituation | null
  /** Varias situaciones (cualquiera de ellas); se suma a `situacion`. */
  situaciones?: OperationalSituation[]
  /** `publicId` de un centro de almacenamiento; null = todos. */
  centro?: string | null
  page: number
  size: number
  direction: "asc" | "desc"
}

export const DEFAULT_TIRES_QUERY: TiresQuery = {
  numeroSerie: "",
  marcaId: null,
  estado: null,
  situacion: null,
  page: 0,
  size: DEFAULT_PAGE_SIZE,
  direction: "asc",
}

export async function searchTires(query: TiresQuery) {
  const { data } = await apiClient.get<Page<TireListItem>>(
    "/api/v1/neumaticos",
    {
      params: {
        numeroSerie: query.numeroSerie || undefined,
        marcaId: query.marcaId ?? undefined,
        estado: query.estado ?? undefined,
        situacion: situationsParam(query),
        centro: query.centro ?? undefined,
        page: query.page,
        size: query.size,
        sort: `numeroSerie,${query.direction}`,
      },
      // `situacion` repetible: situacion=EN_STOCK&situacion=EN_ESPERA (sin []).
      paramsSerializer: { indexes: null },
    }
  )
  return data
}

function situationsParam(query: TiresQuery) {
  const situations = [
    ...(query.situacion ? [query.situacion] : []),
    ...(query.situaciones ?? []),
  ]
  return situations.length > 0 ? situations : undefined
}

export async function registerTire(values: TireFormValues) {
  const { data } = await apiClient.post<Tire>("/api/v1/neumaticos", values)
  return data
}

export async function getTireBrands() {
  const { data } = await apiClient.get<TireBrand[]>("/api/v1/neumaticos/marcas")
  return data
}

/**
 * Ubicación operativa vigente (`UbicacionOperativaDTO` del backend): solo viene
 * lo que corresponde a la situación (montado, en stock o en reparación).
 */
export type TireLocation = {
  unidad: string | null
  posicion: string | null
  centroAlmacenamiento: string | null
  servicioReparacion: string | null
}

/** Ficha de un neumático (`NeumaticoDetalleDTO` del backend). */
export type TireDetail = {
  publicId: string
  numeroSerie: string | null
  marca: string
  estadoActual: TireStatus
  situacionOperativaVigente: OperationalSituation
  kilometrajeTotal: number | null
  kilometrajeUltimaReparacion: number | null
  tipoOrigen: AxleType
  tipoActual: AxleType
  cantidadRecapados: number | null
  comentario: string | null
  ubicacion: TireLocation
}

export async function getTire(publicId: string) {
  const { data } = await apiClient.get<TireDetail>(
    `/api/v1/neumaticos/${publicId}`
  )
  return data
}
