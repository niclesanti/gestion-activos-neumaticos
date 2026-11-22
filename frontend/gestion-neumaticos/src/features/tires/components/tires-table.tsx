import * as React from "react"

import { CircleDotIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { SortableTableHead } from "@/components/common/sortable-table-head"
import { TablePagination } from "@/components/common/table-pagination"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  DEFAULT_TIRES_QUERY,
  type TiresQuery,
} from "@/features/tires/api/tires-api"
import {
  TireFilters,
  type TireFilterValues,
} from "@/features/tires/components/tire-filters"
import { useTires } from "@/features/tires/hooks/use-tires"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { cn } from "@/lib/utils"

const NO_FILTERS: TireFilterValues = {
  search: "",
  marcaId: null,
  estado: null,
  situacion: null,
}

/**
 * Listado de neumáticos activos con filtros por número de serie, marca,
 * estado y situación operativa. Solo lectura: el filtrado, el orden y la
 * paginación los resuelve el servidor.
 */
export function TiresTable() {
  const { t, i18n } = useTranslation("tires")
  const [filters, setFilters] = React.useState<TireFilterValues>(NO_FILTERS)
  const [view, setView] = React.useState<
    Pick<TiresQuery, "page" | "direction">
  >({
    page: DEFAULT_TIRES_QUERY.page,
    direction: DEFAULT_TIRES_QUERY.direction,
  })
  const debouncedSearch = useDebouncedValue(filters.search)

  const query: TiresQuery = {
    ...DEFAULT_TIRES_QUERY,
    numeroSerie: debouncedSearch.trim(),
    marcaId: filters.marcaId,
    estado: filters.estado,
    situacion: filters.situacion,
    ...view,
  }
  const { data, isPending, isError, isFetching, refetch } = useTires(query)

  function changeFilters(next: TireFilterValues) {
    setFilters(next)
    setView((current) => ({ ...current, page: 0 }))
  }

  function clearFilters() {
    changeFilters(NO_FILTERS)
  }

  function toggleSort() {
    setView((current) => ({
      direction: current.direction === "asc" ? "desc" : "asc",
      page: 0,
    }))
  }

  function goToPage(page: number) {
    setView((current) => ({ ...current, page }))
  }

  const tires = data?.content ?? []
  const pageInfo = data?.page
  const hasActiveFilters =
    filters.search.trim() !== "" ||
    filters.marcaId !== null ||
    filters.estado !== null ||
    filters.situacion !== null
  const columnCount = 5

  return (
    <div data-slot="tires-table" className="flex flex-col">
      <TireFilters
        values={filters}
        hasActiveFilters={hasActiveFilters}
        onChange={changeFilters}
        onClear={clearFilters}
      />

      {isError ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyTitle>{t("list.error.title")}</EmptyTitle>
            <EmptyDescription>{t("list.error.description")}</EmptyDescription>
          </EmptyHeader>
          <Button type="button" variant="outline" onClick={() => refetch()}>
            {t("list.error.retry")}
          </Button>
        </Empty>
      ) : !isPending && tires.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <CircleDotIcon />
            </EmptyMedia>
            <EmptyTitle>
              {hasActiveFilters
                ? t("list.noResults.title")
                : t("list.empty.title")}
            </EmptyTitle>
            <EmptyDescription>
              {hasActiveFilters
                ? t("list.noResults.description")
                : t("list.empty.description")}
            </EmptyDescription>
          </EmptyHeader>
          {hasActiveFilters ? (
            <Button type="button" variant="outline" onClick={clearFilters}>
              {t("filters.clear")}
            </Button>
          ) : null}
        </Empty>
      ) : (
        <div
          className={cn(
            "rounded-xl border transition-opacity",
            isFetching && !isPending && "opacity-70"
          )}
          aria-busy={isFetching}
        >
          <Table>
            <TableHeader>
              <TableRow>
                <SortableTableHead
                  field="numeroSerie"
                  label={t("list.columns.serialNumber")}
                  activeField="numeroSerie"
                  direction={view.direction}
                  onToggle={toggleSort}
                />
                <TableHead>{t("list.columns.brand")}</TableHead>
                <TableHead>{t("list.columns.status")}</TableHead>
                <TableHead>{t("list.columns.situation")}</TableHead>
                <TableHead>{t("list.columns.mileage")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isPending
                ? Array.from({ length: 5 }, (_, index) => (
                    <TableRow key={index}>
                      {Array.from({ length: columnCount }, (_, cell) => (
                        <TableCell key={cell}>
                          <Skeleton className="h-5 w-full max-w-40" />
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                : tires.map((tire) => (
                    <TableRow key={tire.publicId}>
                      <TableCell className="font-medium">
                        {tire.numeroSerie ?? (
                          <span className="font-normal text-muted-foreground">
                            {t("list.noSerialNumber")}
                          </span>
                        )}
                      </TableCell>
                      <TableCell>{tire.marca}</TableCell>
                      <TableCell>
                        <Badge variant="outline">
                          {t(`status.${tire.estadoActual}`)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {t(`situation.${tire.situacionOperativaVigente}`)}
                      </TableCell>
                      <TableCell>
                        {tire.kilometrajeTotal === null ? (
                          <span className="text-muted-foreground">
                            {t("list.mileageUnknown")}
                          </span>
                        ) : (
                          t("list.mileageValue", {
                            value: tire.kilometrajeTotal.toLocaleString(
                              i18n.language
                            ),
                          })
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
            </TableBody>
          </Table>
        </div>
      )}

      {pageInfo ? (
        <TablePagination
          page={pageInfo.number}
          totalPages={pageInfo.totalPages}
          totalElements={pageInfo.totalElements}
          summary={t("list.pagination.summary", {
            count: pageInfo.totalElements,
            page: pageInfo.number + 1,
            pages: pageInfo.totalPages,
          })}
          onPageChange={goToPage}
        />
      ) : null}
    </div>
  )
}
