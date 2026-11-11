import * as React from "react"

import { ScrollTextIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { SortableTableHead } from "@/components/common/sortable-table-head"
import { TablePagination } from "@/components/common/table-pagination"
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
  DEFAULT_AUDIT_QUERY,
  isAuditAvailable,
  type AuditedEntity,
  type AuditQuery,
  type AuditRecord,
  type AuditSortField,
} from "@/features/audit/api/audit-api"
import { AuditDate } from "@/features/audit/components/audit-date"
import { AuditRecordSheet } from "@/features/audit/components/audit-record-sheet"
import { AuditStatusBadge } from "@/features/audit/components/audit-status-badge"
import { AuditToolbar } from "@/features/audit/components/audit-toolbar"
import { AuditorLabel } from "@/features/audit/components/auditor-label"
import { useAuditRecords } from "@/features/audit/hooks/use-audit-records"
import {
  EMPTY_AUDIT_FILTERS,
  hasActiveFilters,
  type AuditFilters,
} from "@/features/audit/lib/audit-filters"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import { cn } from "@/lib/utils"

/**
 * Historial de auditoría de una entidad: quién y cuándo creó, modificó y dio de
 * baja cada registro. Solo lectura; el filtrado, el orden y la paginación los
 * resuelve el servidor.
 */
export function AuditRecordsTable({
  entity,
  active,
}: {
  entity: AuditedEntity
  /** Solo consulta mientras su pestaña está a la vista. */
  active: boolean
}) {
  const { t } = useTranslation("audit")
  const [filters, setFilters] = React.useState<AuditFilters>(EMPTY_AUDIT_FILTERS)
  const [sort, setSort] = React.useState<
    Pick<AuditQuery, "sort" | "direction" | "page">
  >({
    sort: DEFAULT_AUDIT_QUERY.sort,
    direction: DEFAULT_AUDIT_QUERY.direction,
    page: 0,
  })
  const [selected, setSelected] = React.useState<AuditRecord | null>(null)
  const debouncedQ = useDebouncedValue(filters.q)

  const query: AuditQuery = {
    ...DEFAULT_AUDIT_QUERY,
    ...filters,
    q: debouncedQ.trim(),
    ...sort,
  }
  const { data, isPending, isError, isFetching, refetch } = useAuditRecords(
    entity,
    query,
    { enabled: active }
  )

  if (!isAuditAvailable(entity)) {
    return (
      <Empty className="mt-4 border">
        <EmptyHeader>
          <EmptyMedia variant="icon">
            <ScrollTextIcon />
          </EmptyMedia>
          <EmptyTitle>{t("unavailable.title")}</EmptyTitle>
          <EmptyDescription>{t("unavailable.description")}</EmptyDescription>
        </EmptyHeader>
      </Empty>
    )
  }

  function changeFilters(next: AuditFilters) {
    setFilters(next)
    setSort((current) => ({ ...current, page: 0 }))
  }

  function toggleSort(field: AuditSortField) {
    setSort((current) => ({
      sort: field,
      direction:
        current.sort === field && current.direction === "desc" ? "asc" : "desc",
      page: 0,
    }))
  }

  function goToPage(page: number) {
    setSort((current) => ({ ...current, page }))
  }

  const records = data?.content ?? []
  const pageInfo = data?.page
  const hasFilters = hasActiveFilters(filters)

  return (
    <div data-slot="audit-records" className="flex flex-col">
      <AuditToolbar
        idPrefix={`audit-${entity}`}
        filters={filters}
        onChange={changeFilters}
      />

      {isError ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyTitle>{t("error.title")}</EmptyTitle>
            <EmptyDescription>{t("error.description")}</EmptyDescription>
          </EmptyHeader>
          <Button type="button" variant="outline" onClick={() => refetch()}>
            {t("error.retry")}
          </Button>
        </Empty>
      ) : !isPending && records.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <ScrollTextIcon />
            </EmptyMedia>
            <EmptyTitle>
              {hasFilters ? t("noResults.title") : t("empty.title")}
            </EmptyTitle>
            <EmptyDescription>
              {hasFilters
                ? t("noResults.description")
                : t("empty.description")}
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
                <TableHead>{t("columns.record")}</TableHead>
                <SortableTableHead
                  field="createdAt"
                  label={t("columns.created")}
                  activeField={sort.sort}
                  direction={sort.direction}
                  onToggle={toggleSort}
                />
                <SortableTableHead
                  field="updatedAt"
                  label={t("columns.modified")}
                  activeField={sort.sort}
                  direction={sort.direction}
                  onToggle={toggleSort}
                />
                <TableHead>{t("columns.status")}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isPending
                ? Array.from({ length: 5 }, (_, index) => (
                    <TableRow key={index}>
                      {Array.from({ length: 4 }, (_, cell) => (
                        <TableCell key={cell}>
                          <Skeleton className="h-5 w-full max-w-40" />
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                : records.map((record) => (
                    <TableRow
                      key={record.publicId}
                      className="cursor-pointer"
                      onClick={() => setSelected(record)}
                    >
                      <TableCell className="max-w-64 font-medium">
                        <button
                          type="button"
                          className="max-w-full truncate rounded-sm text-left underline-offset-4 outline-none hover:underline focus-visible:ring-[3px] focus-visible:ring-ring/50"
                          onClick={(event) => {
                            event.stopPropagation()
                            setSelected(record)
                          }}
                        >
                          {record.descripcion}
                        </button>
                      </TableCell>
                      <TableCell>
                        <WhenAndWho at={record.creadoEn} by={record.creadoPor} />
                      </TableCell>
                      <TableCell>
                        {record.modificadoEn === record.creadoEn ? (
                          <span className="text-muted-foreground">
                            {t("unmodified")}
                          </span>
                        ) : (
                          <WhenAndWho
                            at={record.modificadoEn}
                            by={record.modificadoPor}
                          />
                        )}
                      </TableCell>
                      <TableCell>
                        <AuditStatusBadge status={record.estado} />
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
          summary={t("pagination.summary", {
            count: pageInfo.totalElements,
            page: pageInfo.number + 1,
            pages: pageInfo.totalPages,
          })}
          onPageChange={goToPage}
        />
      ) : null}

      <AuditRecordSheet record={selected} onClose={() => setSelected(null)} />
    </div>
  )
}

/** Fecha relativa y usuario de una operación, en una celda. */
function WhenAndWho({
  at,
  by,
}: {
  at: string
  by: AuditRecord["creadoPor"]
}) {
  return (
    <div className="flex min-w-0 flex-col gap-0.5">
      <AuditDate iso={at} />
      <div className="text-sm text-muted-foreground">
        <AuditorLabel auditor={by} />
      </div>
    </div>
  )
}
