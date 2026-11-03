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
