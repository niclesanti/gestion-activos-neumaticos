import { SearchIcon, XIcon } from "lucide-react"
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
import {
  OPERATIONAL_SITUATIONS,
  type OperationalSituation,
} from "@/features/tires/api/tires-api"
import { useTireBrands } from "@/features/tires/hooks/use-tire-brands"
import {
  REGISTRABLE_TIRE_STATUSES,
  type TireStatus,
} from "@/features/tires/schemas/tire-schema"

/** Valor del `Select` que representa "sin filtro". */
const ALL = "ALL"

export type TireFilterValues = {
  search: string
  marcaId: number | null
  estado: TireStatus | null
  situacion: OperationalSituation | null
}

type Option = { value: string; label: string }

/**
 * Barra de filtros del listado de neumáticos. No guarda estado: el padre lo
 * controla y vuelve a la primera página en cada cambio.
 */
export function TireFilters({
  values,
  hasActiveFilters,
  onChange,
  onClear,
}: {
  values: TireFilterValues
  hasActiveFilters: boolean
  onChange: (values: TireFilterValues) => void
  onClear: () => void
}) {
  const { t } = useTranslation("tires")
  const brands = useTireBrands()

  const brandOptions: Option[] = [
    { value: ALL, label: t("filters.brand.all") },
    ...(brands.data ?? []).map((brand) => ({
      value: String(brand.id),
      label: brand.nombre,
    })),
  ]
  // DE_BAJA no se ofrece: un neumático de baja nunca está activo.
  const statusOptions: Option[] = [
    { value: ALL, label: t("filters.status.all") },
    ...REGISTRABLE_TIRE_STATUSES.map((value) => ({
      value,
      label: t(`status.${value}`),
    })),
  ]
  const situationOptions: Option[] = [
    { value: ALL, label: t("filters.situation.all") },
    ...OPERATIONAL_SITUATIONS.filter((value) => value !== "DE_BAJA").map(
      (value) => ({ value, label: t(`situation.${value}`) })
    ),
  ]

  return (
    <div
      data-slot="tire-filters"
      className="flex flex-col gap-3 pb-4 lg:flex-row lg:flex-wrap lg:items-end"
    >
      <div className="flex flex-col gap-1.5 lg:w-64">
        <label htmlFor="tires-search" className="text-sm font-medium">
          {t("filters.search.label")}
        </label>
        <div className="relative">
          <SearchIcon
            aria-hidden="true"
            className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            id="tires-search"
            type="search"
            maxLength={100}
            className="pl-8"
            value={values.search}
            placeholder={t("filters.search.placeholder")}
            onChange={(event) =>
              onChange({ ...values, search: event.target.value })
            }
          />
        </div>
      </div>

      <FilterSelect
        id="tires-brand"
        label={t("filters.brand.label")}
        options={brandOptions}
        value={values.marcaId === null ? ALL : String(values.marcaId)}
        disabled={brands.isPending || brands.isError}
        onChange={(next) =>
          onChange({ ...values, marcaId: next === ALL ? null : Number(next) })
        }
      />
      <FilterSelect
        id="tires-status"
        label={t("filters.status.label")}
        options={statusOptions}
        value={values.estado ?? ALL}
        onChange={(next) =>
          onChange({
            ...values,
            estado: next === ALL ? null : (next as TireStatus),
          })
        }
      />
      <FilterSelect
        id="tires-situation"
        label={t("filters.situation.label")}
        options={situationOptions}
        value={values.situacion ?? ALL}
        onChange={(next) =>
          onChange({
            ...values,
            situacion: next === ALL ? null : (next as OperationalSituation),
          })
        }
      />

      {hasActiveFilters ? (
        <Button type="button" variant="ghost" onClick={onClear}>
          <XIcon data-icon="inline-start" />
          {t("filters.clear")}
        </Button>
      ) : null}
    </div>
  )
}

function FilterSelect({
  id,
  label,
  options,
  value,
  disabled = false,
  onChange,
}: {
  id: string
  label: string
  options: Option[]
  value: string
  disabled?: boolean
  onChange: (value: string) => void
}) {
  return (
    <div className="flex flex-col gap-1.5 lg:w-48">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
      </label>
      <Select
        id={id}
        items={options}
        value={value}
        disabled={disabled}
        onValueChange={(next) => onChange(next ?? ALL)}
      >
        <SelectTrigger className="w-full">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {options.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
