import { XIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import type { AuditStatusFilter } from "@/features/audit/api/audit-api"
import {
  EMPTY_AUDIT_FILTERS,
  hasActiveFilters,
  type AuditFilters,
} from "@/features/audit/lib/audit-filters"

const STATUS_OPTIONS: AuditStatusFilter[] = ["TODOS", "ACTIVOS", "BAJAS"]

/** Búsqueda por texto, estado y rango de fechas de última modificación. */
export function AuditToolbar({
  idPrefix,
  filters,
  onChange,
}: {
  idPrefix: string
  filters: AuditFilters
  onChange: (filters: AuditFilters) => void
}) {
  const { t } = useTranslation("audit")
  const hasFilters = hasActiveFilters(filters)
  const statusItems = STATUS_OPTIONS.map((value) => ({
    value,
    label: t(`filters.status.${value}`),
  }))

  return (
    <div
      data-slot="audit-toolbar"
      className="flex flex-col gap-3 py-4 lg:flex-row lg:flex-wrap lg:items-end"
    >
      <div className="flex min-w-0 flex-1 flex-col gap-1.5 lg:min-w-64">
        <label htmlFor={`${idPrefix}-q`} className="text-sm font-medium">
          {t("filters.search")}
        </label>
        <Input
          id={`${idPrefix}-q`}
          type="search"
          value={filters.q}
          placeholder={t("filters.searchPlaceholder")}
          onChange={(event) => onChange({ ...filters, q: event.target.value })}
        />
      </div>

      <div className="flex flex-col gap-1.5 lg:w-44">
        <label htmlFor={`${idPrefix}-estado`} className="text-sm font-medium">
          {t("filters.statusLabel")}
        </label>
        <Select
          id={`${idPrefix}-estado`}
          items={statusItems}
          value={filters.estado}
          onValueChange={(next) =>
            onChange({ ...filters, estado: (next ?? "TODOS") as AuditStatusFilter })
          }
        >
          <SelectTrigger className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {statusItems.map((item) => (
              <SelectItem key={item.value} value={item.value}>
                {item.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor={`${idPrefix}-desde`} className="text-sm font-medium">
          {t("filters.from")}
        </label>
        <Input
          id={`${idPrefix}-desde`}
          type="date"
          value={filters.desde}
          max={filters.hasta || undefined}
          onChange={(event) =>
            onChange({ ...filters, desde: event.target.value })
          }
        />
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor={`${idPrefix}-hasta`} className="text-sm font-medium">
          {t("filters.to")}
        </label>
        <Input
          id={`${idPrefix}-hasta`}
          type="date"
          value={filters.hasta}
          min={filters.desde || undefined}
          onChange={(event) =>
            onChange({ ...filters, hasta: event.target.value })
          }
        />
      </div>

      {hasFilters ? (
        <Button
          type="button"
          variant="ghost"
          onClick={() => onChange(EMPTY_AUDIT_FILTERS)}
        >
          <XIcon data-icon="inline-start" />
          {t("filters.clear")}
        </Button>
      ) : null}
    </div>
  )
}
