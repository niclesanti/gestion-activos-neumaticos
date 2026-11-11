import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { Button } from "@/components/ui/button"

/**
 * Pie de paginación de una tabla paginada por el servidor: resumen y botones
 * Anterior / Siguiente. No renderiza nada si no hay elementos.
 */
export function TablePagination({
  page,
  totalPages,
  totalElements,
  summary,
  onPageChange,
}: {
  /** Número de página, base 0 (el `page.number` de `Page<T>`). */
  page: number
  totalPages: number
  totalElements: number
  /** Resumen ya traducido por el feature ("12 unidades · página 1 de 2"). */
  summary: string
  onPageChange: (page: number) => void
}) {
  const { t } = useTranslation("common")

  if (totalElements === 0) return null

  return (
    <div
      data-slot="table-pagination"
      className="flex flex-col gap-2 pt-4 sm:flex-row sm:items-center sm:justify-between"
    >
      <p className="text-sm text-muted-foreground" aria-live="polite">
        {summary}
      </p>
      <div className="flex gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeftIcon data-icon="inline-start" />
          {t("pagination.previous")}
        </Button>
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          {t("pagination.next")}
          <ChevronRightIcon data-icon="inline-end" />
        </Button>
      </div>
    </div>
  )
}
