import type * as React from "react"

import { zodResolver } from "@hookform/resolvers/zod"
import type { ParseKeys } from "i18next"
import { Controller, useForm, useWatch } from "react-hook-form"
import { useTranslation } from "react-i18next"

import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  FieldLegend,
  FieldSet,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { useProvinces } from "@/features/suppliers/hooks/use-provinces"
import {
  hasAddress,
  supplierSchema,
  type SupplierFormInput,
  type SupplierFormValues,
} from "@/features/suppliers/schemas/supplier-schema"

/** Los mensajes de zod son claves del namespace `suppliers` (ver supplier-schema). */
type SuppliersKey = ParseKeys<"suppliers">

type FieldName = keyof SupplierFormInput

type Option = { value: string; label: string }

/** Para marcar en un campo un error que devolvió el backend (ej. 409). */
export type SupplierFormHelpers = {
  setServerError: (field: FieldName, messageKey: SuppliersKey) => void
}

type SupplierFormProps = {
  /** Id del `<form>`: el botón de envío puede vivir afuera (ej. en el footer del dialog). */
  id: string
  defaultValues?: Partial<SupplierFormInput>
  disabled?: boolean
  onSubmit: (values: SupplierFormValues, helpers: SupplierFormHelpers) => void
}

const EMPTY_SUPPLIER: SupplierFormInput = {
  nombre: "",
  email: "",
  telefono: "",
  calle: "",
  altura: "",
  piso: "",
  departamento: "",
  provinciaId: "",
  localidad: "",
  codigoPostal: "",
}

/**
 * Formulario de un proveedor, pensado para el alta y la futura modificación.
 * Valida exactamente lo mismo que `ProveedorRequestDTO` (ver supplier-schema) y
 * entrega el cuerpo listo para la API. Contacto y domicilio son opcionales; si
 * se empieza a cargar el domicilio, sus datos principales pasan a ser obligatorios.
 */
export function SupplierForm({
  id,
  defaultValues,
  disabled = false,
  onSubmit,
}: SupplierFormProps) {
  const { t } = useTranslation("suppliers")
  const provinces = useProvinces()

  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<SupplierFormInput, unknown, SupplierFormValues>({
    resolver: zodResolver(supplierSchema),
    defaultValues: { ...EMPTY_SUPPLIER, ...defaultValues },
  })

  const values = useWatch({ control })
  // El asterisco del domicilio aparece recién cuando se empieza a cargar uno.
  const addressRequired = hasAddress({ ...EMPTY_SUPPLIER, ...values })

  const provinceOptions: Option[] = (provinces.data ?? []).map((province) => ({
    value: String(province.id),
    label: province.nombre,
  }))

  const errorOf = (name: FieldName) => {
    const message = errors[name]?.message
    return message ? t(message as SuppliersKey) : undefined
  }

  const helpers: SupplierFormHelpers = {
    setServerError: (field, messageKey) =>
      setError(
        field,
        { type: "server", message: messageKey },
        { shouldFocus: true }
      ),
  }

  return (
    <form
      id={id}
      noValidate
      onSubmit={handleSubmit((values) => onSubmit(values, helpers))}
    >
      <fieldset disabled={disabled} className="flex flex-col gap-6">
        <TextField
          id={`${id}-name`}
          label={t("form.name.label")}
          error={errorOf("nombre")}
          required
        >
          <Input
            id={`${id}-name`}
            placeholder={t("form.name.placeholder")}
            autoComplete="off"
            maxLength={150}
            {...ariaFor(`${id}-name`, errorOf("nombre"), true)}
            {...register("nombre")}
          />
        </TextField>

        <Separator />

        <FieldSet className="gap-0">
          <FieldLegend>{t("form.contact.legend")}</FieldLegend>
          <FieldDescription className="-mt-2 mb-4">
            {t("form.contact.description")}
          </FieldDescription>
          <div className="grid gap-x-4 gap-y-5 sm:grid-cols-2">
            <TextField
              id={`${id}-email`}
              label={t("form.email.label")}
              error={errorOf("email")}
            >
              <Input
                id={`${id}-email`}
                type="email"
                inputMode="email"
                placeholder={t("form.email.placeholder")}
                autoComplete="off"
                spellCheck={false}
                maxLength={254}
                {...ariaFor(`${id}-email`, errorOf("email"))}
                {...register("email")}
              />
            </TextField>

            <TextField
              id={`${id}-phone`}
              label={t("form.phone.label")}
              error={errorOf("telefono")}
            >
              <Input
                id={`${id}-phone`}
                type="tel"
                inputMode="tel"
                placeholder={t("form.phone.placeholder")}
                autoComplete="off"
                maxLength={30}
                {...ariaFor(`${id}-phone`, errorOf("telefono"))}
                {...register("telefono")}
              />
            </TextField>
          </div>
        </FieldSet>

        <Separator />

        <FieldSet className="gap-0">
          <FieldLegend>{t("form.address.legend")}</FieldLegend>
          <FieldDescription className="-mt-2 mb-4">
            {t("form.address.description")}
          </FieldDescription>
          <div className="grid gap-x-4 gap-y-5 sm:grid-cols-6">
            <TextField
              id={`${id}-street`}
              className="sm:col-span-4"
              label={t("form.street.label")}
              error={errorOf("calle")}
              required={addressRequired}
            >
              <Input
                id={`${id}-street`}
                placeholder={t("form.street.placeholder")}
                autoComplete="off"
                maxLength={100}
                {...ariaFor(`${id}-street`, errorOf("calle"), addressRequired)}
                {...register("calle")}
              />
            </TextField>

            <TextField
              id={`${id}-number`}
              className="sm:col-span-2"
              label={t("form.number.label")}
              error={errorOf("altura")}
              required={addressRequired}
            >
              <Input
                id={`${id}-number`}
                placeholder={t("form.number.placeholder")}
                autoComplete="off"
                maxLength={10}
                {...ariaFor(`${id}-number`, errorOf("altura"), addressRequired)}
                {...register("altura")}
              />
            </TextField>

            <TextField
              id={`${id}-floor`}
              className="sm:col-span-3"
              label={t("form.floor.label")}
              error={errorOf("piso")}
            >
              <Input
                id={`${id}-floor`}
                placeholder={t("form.floor.placeholder")}
                autoComplete="off"
                maxLength={10}
                {...ariaFor(`${id}-floor`, errorOf("piso"))}
                {...register("piso")}
              />
            </TextField>

            <TextField
              id={`${id}-apartment`}
              className="sm:col-span-3"
              label={t("form.apartment.label")}
              error={errorOf("departamento")}
            >
              <Input
                id={`${id}-apartment`}
                placeholder={t("form.apartment.placeholder")}
                autoComplete="off"
                maxLength={10}
                {...ariaFor(`${id}-apartment`, errorOf("departamento"))}
                {...register("departamento")}
              />
            </TextField>

            <Controller
              control={control}
              name="provinciaId"
              render={({ field }) => (
                <SelectField
                  id={`${id}-province`}
                  className="sm:col-span-3"
                  label={t("form.province.label")}
                  placeholder={
                    provinces.isPending
                      ? t("form.province.loading")
                      : t("form.province.placeholder")
                  }
                  description={
                    provinces.isError ? t("form.province.error") : undefined
                  }
                  options={provinceOptions}
                  value={field.value}
                  onChange={field.onChange}
                  onBlur={field.onBlur}
                  error={errorOf("provinciaId")}
                  disabled={provinces.isPending || provinces.isError}
                  required={addressRequired}
                />
              )}
            />

            <TextField
              id={`${id}-city`}
              className="sm:col-span-3"
              label={t("form.city.label")}
              description={t("form.city.description")}
              error={errorOf("localidad")}
              required={addressRequired}
            >
              <Input
                id={`${id}-city`}
                placeholder={t("form.city.placeholder")}
                autoComplete="off"
                maxLength={100}
                {...ariaFor(
                  `${id}-city`,
                  errorOf("localidad"),
                  addressRequired
                )}
                {...register("localidad")}
              />
            </TextField>

            <TextField
              id={`${id}-postal-code`}
              className="sm:col-span-3"
              label={t("form.postalCode.label")}
              description={t("form.postalCode.description")}
              error={errorOf("codigoPostal")}
            >
              <Input
                id={`${id}-postal-code`}
                placeholder={t("form.postalCode.placeholder")}
                autoComplete="off"
                spellCheck={false}
                maxLength={8}
                {...ariaFor(`${id}-postal-code`, errorOf("codigoPostal"))}
                {...register("codigoPostal")}
              />
            </TextField>
          </div>
        </FieldSet>
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
        value={value || null}
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

/** Hereda el color del label: neutro, y rojo solo cuando el campo tiene error. */
function RequiredMark() {
  return <span aria-hidden>*</span>
}
