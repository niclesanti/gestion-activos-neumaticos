import * as React from "react"

import { CircleDotIcon, SearchIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

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
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Skeleton } from "@/components/ui/skeleton"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import {
  DEFAULT_TIRES_QUERY,
  type TireListItem,
  type TiresQuery,
} from "@/features/tires/api/tires-api"
import {
  FilterSelect,
  type FilterOption,
} from "@/features/tires/components/tire-filters"
import { useStorageCenters } from "@/features/tires/hooks/use-storage-centers"
import { useTires } from "@/features/tires/hooks/use-tires"
import { useDebouncedValue } from "@/hooks/use-debounced-value"
import type { PickedTire, TirePickerProps } from "@/lib/tire-picker"
import { cn } from "@/lib/utils"

type OriginTab = "EN_ESPERA" | "EN_STOCK"

/** Centro del select "En stock": todos o el `publicId` de uno. */
const ALL_CENTERS = "ALL"

function toPicked(tire: TireListItem): PickedTire {
  return {
    publicId: tire.publicId,
    numeroSerie: tire.numeroSerie,
    marca: tire.marca,
    estadoActual: tire.estadoActual,
    situacion:
      tire.situacionOperativaVigente === "EN_STOCK" ? "EN_STOCK" : "EN_ESPERA",
    centroAlmacenamiento: tire.centroAlmacenamiento,
  }
}

/**
 * Buscador de neumáticos disponibles para montar (HU-31): pestañas "En
 * espera" (por defecto) y "En stock" (con filtro por centro), búsqueda por
 * número de serie sobre la pestaña activa y lista paginada con el tamaño y el
 * orden (número de serie) por defecto del backend. Lo compone `router.tsx`
 * dentro del montaje de `transport-units` (contrato en `lib/tire-picker.ts`).
 * Un clic o Enter sobre un neumático lo elige; las flechas solo lo marcan.
 */
export function TirePicker({ selected, onSelect, onConfirm }: TirePickerProps) {
  const { t } = useTranslation("tires")
  const [search, setSearch] = React.useState("")
  // Al abrir, "En espera"; al volver a este paso, la pestaña de lo elegido.
  const [tab, setTab] = React.useState<OriginTab>(
    selected?.situacion ?? "EN_ESPERA"
  )
  const [center, setCenter] = React.useState(ALL_CENTERS)
  const [page, setPage] = React.useState(0)
  const debouncedSearch = useDebouncedValue(search)
  const centers = useStorageCenters()

  const query: TiresQuery = {
    ...DEFAULT_TIRES_QUERY,
    numeroSerie: debouncedSearch.trim(),
    situaciones: [tab],
    centro: tab === "EN_STOCK" && center !== ALL_CENTERS ? center : null,
    page,
    // Sin `size`: tamaño de página por defecto del backend.
    size: undefined,
  }
  const result = useTires(query)
  const hasFilters =
    search.trim() !== "" || (tab === "EN_STOCK" && center !== ALL_CENTERS)

  const centerOptions: FilterOption[] = [
    { value: ALL_CENTERS, label: t("picker.center.all") },
    ...(centers.data ?? []).map((option) => ({
      value: option.publicId,
      label: t("picker.center.option", {
        name: option.nombre,
        count: option.cantidadNeumaticosEnStock,
      }),
    })),
  ]

  const results = (
    <TireResults
      result={result}
      tab={tab}
      hasFilters={hasFilters}
      selected={selected}
      onSelect={onSelect}
      onConfirm={onConfirm}
      onPageChange={setPage}
    />
  )

  return (
    <div data-slot="tire-picker" className="flex flex-col gap-3">
      <div className="flex flex-col gap-1.5">
        <label htmlFor="tire-picker-search" className="text-sm font-medium">
          {t("filters.search.label")}
        </label>
        <div className="relative">
          <SearchIcon
            aria-hidden="true"
            className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground"
          />
          <Input
            id="tire-picker-search"
            type="search"
            // Primer paso del montaje: se escribe sin tener que hacer clic.
            autoFocus
            maxLength={100}
            className="pl-8"
            value={search}
            placeholder={t("filters.search.placeholder")}
            onChange={(event) => {
              setSearch(event.target.value)
              setPage(0)
            }}
          />
        </div>
      </div>

      <Tabs
        value={tab}
        onValueChange={(next) => {
          setTab(next as OriginTab)
          setPage(0)
        }}
        className="gap-3"
      >
        <TabsList aria-label={t("picker.tabs.label")} className="w-full">
          <TabsTrigger value="EN_ESPERA">
            {t("picker.tabs.waiting")}
          </TabsTrigger>
          <TabsTrigger value="EN_STOCK">{t("picker.tabs.stock")}</TabsTrigger>
        </TabsList>
        <TabsContent value="EN_ESPERA" className="flex flex-col gap-3">
          {results}
        </TabsContent>
        <TabsContent value="EN_STOCK" className="flex flex-col gap-3">
          <FilterSelect
            id="tire-picker-center"
            label={t("picker.center.label")}
            options={centerOptions}
            value={center}
            className="lg:w-full"
            onChange={(next) => {
              setCenter(next)
              setPage(0)
            }}
          />
          {results}
        </TabsContent>
      </Tabs>
    </div>
  )
}

/** Lista paginada de la pestaña activa, con sus estados de carga, vacío y error. */
function TireResults({
  result,
  tab,
  hasFilters,
  selected,
  onSelect,
  onConfirm,
  onPageChange,
}: TirePickerProps & {
  result: ReturnType<typeof useTires>
  tab: OriginTab
  hasFilters: boolean
  onPageChange: (page: number) => void
}) {
  const { t, i18n } = useTranslation("tires")
  const { data, isPending, isError, isFetching, refetch } = result
  const tires = data?.content ?? []
  const byId = new Map(tires.map((tire) => [tire.publicId, tire]))

  function pick(publicId: string, confirm: boolean) {
    const tire = byId.get(publicId)
    if (!tire) return
    const picked = toPicked(tire)
    onSelect(picked)
    if (confirm) onConfirm(picked)
  }

  return (
    <>
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
      ) : isPending ? (
        <div className="flex flex-col gap-2" aria-busy="true">
          {Array.from({ length: 4 }, (_, index) => (
            <Skeleton key={index} className="h-14 w-full rounded-2xl" />
          ))}
        </div>
      ) : tires.length === 0 ? (
        <Empty className="border">
          <EmptyHeader>
            <EmptyMedia variant="icon">
              <CircleDotIcon />
            </EmptyMedia>
            <EmptyTitle>
              {hasFilters
                ? t("picker.noResults.title")
                : tab === "EN_ESPERA"
                  ? t("picker.empty.waiting")
                  : t("picker.empty.stock")}
            </EmptyTitle>
            <EmptyDescription>
              {hasFilters
                ? t("picker.noResults.description")
                : t("picker.empty.description")}
            </EmptyDescription>
          </EmptyHeader>
        </Empty>
      ) : (
        <RadioGroup
          aria-label={t("picker.listLabel")}
          aria-busy={isFetching}
          value={selected?.publicId ?? null}
          onValueChange={(value) => pick(String(value), false)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && selected) {
              event.preventDefault()
              onConfirm(selected)
            }
          }}
          className={cn("gap-2", isFetching && "opacity-70")}
        >
          {tires.map((tire) => {
            const id = `tire-option-${tire.publicId}`
            return (
              <label
                key={tire.publicId}
                htmlFor={id}
                // detail > 0: clic real (el teclado dispara clics con detail 0).
                onClick={(event) => {
                  if (event.detail > 0) pick(tire.publicId, true)
                }}
                className="flex cursor-pointer items-center gap-3 rounded-2xl border px-3 py-2.5 transition-colors hover:bg-muted/50 has-data-checked:border-primary has-data-checked:bg-primary/5"
              >
                <RadioGroupItem id={id} value={tire.publicId} />
                <span className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className="truncate font-mono text-sm">
                    {tire.numeroSerie ?? t("list.noSerialNumber")}
                  </span>
                  <span className="truncate text-xs text-muted-foreground">
                    {tire.marca} · {t(`status.${tire.estadoActual}`)}
                    {tire.kilometrajeTotal === null
                      ? null
                      : ` · ${t("list.mileageValue", {
                          value: tire.kilometrajeTotal.toLocaleString(
                            i18n.language
                          ),
                        })}`}
                  </span>
                </span>
                {tire.centroAlmacenamiento ? (
                  <Badge variant="outline" className="max-w-32 shrink-0">
                    <span className="truncate">
                      {tire.centroAlmacenamiento}
                    </span>
                  </Badge>
                ) : null}
              </label>
            )
          })}
        </RadioGroup>
      )}

      {data ? (
        <TablePagination
          page={data.page.number}
          totalPages={data.page.totalPages}
          totalElements={data.page.totalElements}
          summary={t("list.pagination.summary", {
            count: data.page.totalElements,
            page: data.page.number + 1,
            pages: Math.max(data.page.totalPages, 1),
          })}
          onPageChange={onPageChange}
        />
      ) : null}
    </>
  )
}
