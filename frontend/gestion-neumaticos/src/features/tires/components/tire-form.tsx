import * as React from "react"

import { zodResolver } from "@hookform/resolvers/zod"
import type { ParseKeys } from "i18next"
import { Controller, useForm, useWatch } from "react-hook-form"
import { useTranslation } from "react-i18next"

import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import { useSuppliers } from "@/features/tires/hooks/use-suppliers"
import { useTireBrands } from "@/features/tires/hooks/use-tire-brands"
import {
  AXLE_TYPES,
  REGISTRABLE_TIRE_STATUSES,
  tireSchema,
  type TireFormInput,
  type TireFormValues,
} from "@/features/tires/schemas/tire-schema"

/** Los mensajes de zod son claves del namespace `tires` (ver tire-schema). */
type TiresKey = ParseKeys<"tires">

type FieldName = keyof TireFormInput

/** Para marcar en un campo un error que devolvió el backend (ej. 409). */
export type TireFormHelpers = {
  setServerError: (field: FieldName, messageKey: TiresKey) => void
}

type TireFormProps = {
  /** Id del `<form>`: el botón de envío puede vivir afuera (ej. en el footer del dialog). */
  id: string
  /**
   * En "edit" el número de serie es de solo lectura (no se puede modificar
   * después del registro) y no se pide proveedor (solo aplica a la compra).
   */
  mode: "create" | "edit"
  defaultValues?: Partial<TireFormInput>
  disabled?: boolean
  onSubmit: (values: TireFormValues, helpers: TireFormHelpers) => void
}

const EMPTY_TIRE: Partial<TireFormInput> = {
  numeroSerie: "",
  marcaId: "",
  proveedorId: "",
  comentario: "",
}

const NEW_TIRE_USAGE_FIELDS = [
  "kilometrajeTotal",
  "kilometrajeUltimaReparacion",
  "cantidadRecapados",
] as const

type Option = { value: string; label: string }

/**
 * Formulario de un neumático, compartido por el alta y la futura modificación.
 * Valida exactamente lo mismo que `NeumaticoRequestDTO` (ver tire-schema) y
 * entrega el cuerpo listo para la API.
 */
export function TireForm({
  id,
  mode,
  defaultValues,
  disabled = false,
  onSubmit,
}: TireFormProps) {
  const { t } = useTranslation("tires")
  const brands = useTireBrands()
  const suppliers = useSuppliers({ enabled: mode === "create" })

  const {
    control,
    register,
    handleSubmit,
    setValue,
    setError,
    formState: { errors, isSubmitted },
  } = useForm<TireFormInput, unknown, TireFormValues>({
    resolver: zodResolver(tireSchema),
    defaultValues: { ...EMPTY_TIRE, ...defaultValues },
  })

  const status = useWatch({ control, name: "estadoActual" })
  const isNew = status === "NUEVO"

  const statusOptions: Option[] = REGISTRABLE_TIRE_STATUSES.map((value) => ({
    value,
    label: t(`status.${value}`),
  }))
  const axleOptions: Option[] = AXLE_TYPES.map((value) => ({
    value,
    label: t(`axle.${value}`),
  }))
  const brandOptions: Option[] = (brands.data ?? []).map((brand) => ({
    value: String(brand.id),
    label: brand.nombre,
  }))
  const supplierOptions: Option[] = [
    { value: "", label: t("form.supplier.none") },
    ...(suppliers.data ?? []).map((supplier) => ({
      value: supplier.publicId,
      label: supplier.nombre,
    })),
  ]

  const errorOf = (name: FieldName) => {
    const message = errors[name]?.message
    return message ? t(message as TiresKey) : undefined
  }

  const helpers: TireFormHelpers = {
    setServerError: (field, messageKey) =>
      setError(
        field,
        { type: "server", message: messageKey },
        { shouldFocus: true }
      ),
  }

  // Un neumático nuevo no tiene uso: se completa en 0 (y se valida igual).
  const fillNewTireUsage = () => {
    for (const name of NEW_TIRE_USAGE_FIELDS) {
      setValue(name, 0, { shouldValidate: isSubmitted, shouldDirty: true })
    }
  }

  return (
    <form
      id={id}
      noValidate
      onSubmit={handleSubmit((values) => onSubmit(values, helpers))}
    >
      <fieldset
        disabled={disabled}
        className="grid gap-x-4 gap-y-5 sm:grid-cols-2"
      >
        <TextField
          id={`${id}-serial`}
          className="sm:col-span-2"
          label={t("form.serialNumber.label")}
          description={t("form.serialNumber.description")}
          error={errorOf("numeroSerie")}
        >
          <Input
            id={`${id}-serial`}
            placeholder={t("form.serialNumber.placeholder")}
            autoComplete="off"
            spellCheck={false}
            readOnly={mode === "edit"}
            maxLength={120}
            {...ariaFor(`${id}-serial`, errorOf("numeroSerie"))}
            {...register("numeroSerie")}
          />
        </TextField>

        <Controller
          control={control}
          name="estadoActual"
          render={({ field }) => (
            <SelectField
              id={`${id}-status`}
              label={t("form.status.label")}
              placeholder={t("form.status.placeholder")}
              options={statusOptions}
              value={field.value}
              onChange={(value) => {
                field.onChange(value)
                if (value === "NUEVO") fillNewTireUsage()
              }}
              onBlur={field.onBlur}
              error={errorOf("estadoActual")}
              required
            />
          )}
        />

        <Controller
          control={control}
          name="marcaId"
          render={({ field }) => (
            <SelectField
              id={`${id}-brand`}
              label={t("form.brand.label")}
              placeholder={
                brands.isPending
                  ? t("form.brand.loading")
                  : t("form.brand.placeholder")
              }
              options={brandOptions}
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              error={
                errorOf("marcaId") ??
                (brands.isError ? t("form.brand.error") : undefined)
              }
              disabled={brands.isPending || brands.isError}
              required
            />
          )}
        />

        {mode === "create" && isNew ? (
          <Controller
            control={control}
            name="proveedorId"
            render={({ field }) => (
              <SelectField
                id={`${id}-supplier`}
                className="sm:col-span-2"
                label={t("form.supplier.label")}
                placeholder={
                  suppliers.isPending
                    ? t("form.supplier.loading")
                    : t("form.supplier.placeholder")
                }
                description={
                  suppliers.isError
                    ? t("form.supplier.error")
                    : t("form.supplier.description")
                }
                options={supplierOptions}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                error={errorOf("proveedorId")}
                disabled={suppliers.isPending || suppliers.isError}
              />
            )}
          />
        ) : null}

        <Controller
          control={control}
          name="tipoOrigen"
          render={({ field }) => (
            <SelectField
              id={`${id}-origin-axle`}
              label={t("form.originAxle.label")}
              placeholder={t("form.originAxle.placeholder")}
              options={axleOptions}
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              error={errorOf("tipoOrigen")}
              required
            />
          )}
        />

        <Controller
          control={control}
          name="tipoActual"
          render={({ field }) => (
            <SelectField
              id={`${id}-current-axle`}
              label={t("form.currentAxle.label")}
              placeholder={t("form.currentAxle.placeholder")}
              options={axleOptions}
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              error={errorOf("tipoActual")}
              required
            />
          )}
        />

        <TextField
          id={`${id}-total-km`}
          label={t("form.totalKm.label")}
          error={errorOf("kilometrajeTotal")}
          required
        >
          <Input
            id={`${id}-total-km`}
            type="number"
            inputMode="decimal"
            min={0}
            step="any"
            {...ariaFor(`${id}-total-km`, errorOf("kilometrajeTotal"), true)}
            {...register("kilometrajeTotal", { valueAsNumber: true })}
          />
        </TextField>

        <TextField
          id={`${id}-km-since-repair`}
          label={t("form.kmSinceRepair.label")}
          error={errorOf("kilometrajeUltimaReparacion")}
          required
        >
          <Input
            id={`${id}-km-since-repair`}
            type="number"
            inputMode="decimal"
            min={0}
            step="any"
            {...ariaFor(
              `${id}-km-since-repair`,
              errorOf("kilometrajeUltimaReparacion"),
              true
            )}
            {...register("kilometrajeUltimaReparacion", {
              valueAsNumber: true,
            })}
          />
        </TextField>

        <TextField
          id={`${id}-retreads`}
          label={t("form.retreads.label")}
          description={isNew ? t("form.newTireHint") : undefined}
          error={errorOf("cantidadRecapados")}
          required
        >
          <Input
            id={`${id}-retreads`}
            type="number"
            inputMode="numeric"
            min={0}
            max={20}
            step={1}
            {...ariaFor(`${id}-retreads`, errorOf("cantidadRecapados"), true)}
            {...register("cantidadRecapados", { valueAsNumber: true })}
          />
        </TextField>

        <TextField
          id={`${id}-comment`}
          className="sm:col-span-2"
          label={t("form.comment.label")}
          error={errorOf("comentario")}
        >
          <Textarea
            id={`${id}-comment`}
            placeholder={t("form.comment.placeholder")}
            rows={3}
            {...ariaFor(`${id}-comment`, errorOf("comentario"))}
            {...register("comentario")}
          />
        </TextField>
      </fieldset>
    </form>
  )
}

function ariaFor(id: string, error: string | undefined, required = false) {
  return {
    "aria-invalid": error ? true : undefined,
    "aria-describedby": error ? `${id}-error` : undefined,
    "aria-required": required || undefined,
  }
}

function TextField({
  id,
  label,
  description,
  error,
  required = false,
  className,
  children,
}: {
  id: string
  label: string
  description?: string
  error?: string
  required?: boolean
  className?: string
  children: React.ReactNode
}) {
  return (
    <Field data-invalid={error ? true : undefined} className={className}>
      <FieldLabel htmlFor={id}>
        {label}
        {required ? <RequiredMark /> : null}
      </FieldLabel>
      {children}
      {description ? <FieldDescription>{description}</FieldDescription> : null}
      {error ? <FieldError id={`${id}-error`}>{error}</FieldError> : null}
    </Field>
  )
}

function SelectField({
  id,
  label,
  placeholder,
  description,
  options,
  value,
  onChange,
  onBlur,
  error,
  disabled = false,
  required = false,
  className,
}: {
  id: string
  label: string
  placeholder: string
  description?: string
  options: Option[]
  value: string | undefined
  onChange: (value: string) => void
  onBlur: () => void
  error?: string
  disabled?: boolean
  required?: boolean
  className?: string
}) {
  return (
    <Field data-invalid={error ? true : undefined} className={className}>
      <FieldLabel htmlFor={id}>
        {label}
        {required ? <RequiredMark /> : null}
      </FieldLabel>
      <Select
        id={id}
        items={options}
        value={value ?? null}
        onValueChange={(next) => onChange(next ?? "")}
        disabled={disabled}
      >
        <SelectTrigger
          className="w-full"
          onBlur={onBlur}
          {...ariaFor(id, error, required)}
        >
          <SelectValue placeholder={placeholder} />
        </SelectTrigger>
        <SelectContent>
          {options.map((option) => (
            <SelectItem key={option.value} value={option.value}>
              {option.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {description ? <FieldDescription>{description}</FieldDescription> : null}
      {error ? <FieldError id={`${id}-error`}>{error}</FieldError> : null}
    </Field>
  )
}

function RequiredMark() {
  return (
    <span aria-hidden className="text-destructive">
      *
    </span>
  )
}
