import * as React from "react"

import { SearchIcon, TruckIcon } from "lucide-react"
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
import { Input } from "@/components/ui/input"
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
  DEFAULT_TRANSPORT_UNITS_QUERY,
  type TransportUnitsQuery,
} from "@/features/transport-units/api/transport-units-api"
import { TransportUnitDetailDialog } from "@/features/transport-units/components/transport-unit-detail-dialog"
import { useTransportUnits } from "@/features/transport-units/hooks/use-transport-units"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { cn } from "@/lib/utils"

/**
 * Listado de unidades de transporte activas con búsqueda por patente. Solo
 * lectura: el filtrado, el orden y la paginación los resuelve el servidor.
 */
export function TransportUnitsTable() {
  const { t } = useTranslation("transport-units")
  const [search, setSearch] = React.useState("")
  const [view, setView] = React.useState<
    Pick<TransportUnitsQuery, "page" | "direction">
  >({
    page: DEFAULT_TRANSPORT_UNITS_QUERY.page,
    direction: DEFAULT_TRANSPORT_UNITS_QUERY.direction,
  })
  const [selected, setSelected] = React.useState<{
    publicId: string
    plate: string
  } | null>(null)
  const debouncedSearch = useDebouncedValue(search)

  const query: TransportUnitsQuery = {
    ...DEFAULT_TRANSPORT_UNITS_QUERY,
    patente: debouncedSearch.trim(),
    ...view,
  }
  const { data, isPending, isError, isFetching, refetch } =
    useTransportUnits(query)

  function changeSearch(value: string) {
    setSearch(value)
    setView((current) => ({ ...current, page: 0 }))
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

  const units = data?.content ?? []
  const pageInfo = data?.page
  const hasTerm = query.patente !== ""

  return (
    <div data-slot="transport-units-table" className="flex flex-col">
      <div className="flex flex-col gap-1.5 pb-4 sm:max-w-sm">
        <label htmlFor="transport-units-search" className="text-sm font-medium">
          {t("list.search.label")}
        </label>
        <div className="relative">
          <SearchIcon
            aria-hidden="true"
            className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            id="transport-units-search"
            type="search"
            maxLength={15}
            className="pl-8"
            value={search}
            placeholder={t("list.search.placeholder")}
            onChange={(event) => changeSearch(event.target.value)}
          />
        </div>
      </div>

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
      ) : !isPending && units.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <TruckIcon />
            </EmptyMedia>
            <EmptyTitle>
              {hasTerm ? t("list.noResults.title") : t("list.empty.title")}
            </EmptyTitle>
            <EmptyDescription>
              {hasTerm
                ? t("list.noResults.description")
                : t("list.empty.description")}
            </EmptyDescription>
          </EmptyHeader>
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
                  field="patente"
                  label={t("list.columns.plate")}
                  activeField="patente"
                  direction={view.direction}
                  onToggle={toggleSort}
                />
                <TableHead>{t("list.columns.configuration")}</TableHead>
                <TableHead>{t("list.columns.type")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isPending
                ? Array.from({ length: 5 }, (_, index) => (
                    <TableRow key={index}>
                      {Array.from({ length: 3 }, (_, cell) => (
                        <TableCell key={cell}>
                          <Skeleton className="h-5 w-full max-w-40" />
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                : units.map((unit) => (
                    <TableRow
                      key={unit.publicId}
                      className="cursor-pointer"
                      onClick={() =>
                        setSelected({
                          publicId: unit.publicId,
                          plate: unit.patente,
                        })
                      }
                    >
                      <TableCell className="font-medium">
                        {/* El clic llega a la fila; el botón da el foco y el
                            rol a teclado y lectores de pantalla. */}
                        <button
                          type="button"
                          className="rounded-sm underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
                          aria-label={t("list.openDetail", {
                            plate: unit.patente,
                          })}
                        >
                          {unit.patente}
                        </button>
                      </TableCell>
                      <TableCell>{unit.configuracion.nombre}</TableCell>
                      <TableCell>
                        <Badge variant="outline">
                          {t(`unitTypes.${unit.configuracion.tipoUnidad}`)}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))}
            </TableBody>
          </Table>
        </div>
      )}

      <TransportUnitDetailDialog
        publicId={selected?.publicId ?? null}
        plate={selected?.plate ?? ""}
        onClose={() => setSelected(null)}
      />

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
