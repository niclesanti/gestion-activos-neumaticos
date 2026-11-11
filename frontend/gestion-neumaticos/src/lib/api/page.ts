/**
 * Tamaño de página por defecto de todos los listados paginados. Es el mismo
 * valor que `PaginacionConfig.TAMANIO_PAGINA_POR_DEFECTO` del backend (que
 * además acota cualquier `size` mayor a 20): si cambia uno, cambia el otro.
 */
export const DEFAULT_PAGE_SIZE = 10

/** Página de Spring Data serializada `VIA_DTO` (`PaginacionConfig` del backend). */
export type Page<T> = {
  content: T[]
  page: {
    size: number
    number: number
    totalElements: number
    totalPages: number
  }
}
