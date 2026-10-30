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
import { AxleLayoutPreview } from "@/features/transport-units/components/axle-layout-preview"
import { useUnitConfigurations } from "@/features/transport-units/hooks/use-unit-configurations"
import {
  transportUnitSchema,
  type TransportUnitFormInput,
  type TransportUnitFormValues,
} from "@/features/transport-units/schemas/transport-unit-schema"

/** Los mensajes de zod son claves del namespace `transport-units`. */
type TransportUnitsKey = ParseKeys<"transport-units">

type FieldName = keyof TransportUnitFormInput

/** Para marcar en un campo un error que devolvió el backend (ej. 409). */
export type TransportUnitFormHelpers = {
  setServerError: (field: FieldName, messageKey: TransportUnitsKey) => void
}

type TransportUnitFormProps = {
  /** Id del `<form>`: el botón de envío vive en el footer del dialog. */
  id: string
  disabled?: boolean
  onSubmit: (
    values: TransportUnitFormValues,
    helpers: TransportUnitFormHelpers
  ) => void
}

const EMPTY_UNIT: TransportUnitFormInput = { patente: "", configuracionId: "" }

/**
 * Alta de una unidad de transporte: patente y configuración de ejes. Valida lo
 * mismo que `UnidadTransporteRequestDTO` (ver transport-unit-schema) y muestra
 * el esquema de ejes de la configuración elegida, con las posiciones que se van
 * a generar.
 */
export function TransportUnitForm({
  id,
  disabled = false,
  onSubmit,
}: TransportUnitFormProps) {
  const { t } = useTranslation("transport-units")
  const configurations = useUnitConfigurations()

  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<TransportUnitFormInput, unknown, TransportUnitFormValues>({
    resolver: zodResolver(transportUnitSchema),
    defaultValues: EMPTY_UNIT,
  })

  const configurationId = useWatch({ control, name: "configuracionId" })
  const selected = configurations.data?.find(
    (configuration) => String(configuration.id) === configurationId
  )

  const options = (configurations.data ?? []).map((configuration) => ({
    value: String(configuration.id),
    label: t("form.configuration.option", {
      name: configuration.nombre,
      slots: configuration.totalSlots,
    }),
  }))

  const errorOf = (name: FieldName) => {
    const message = errors[name]?.message
    return message ? t(message as TransportUnitsKey) : undefined
  }

  const helpers: TransportUnitFormHelpers = {
    setServerError: (field, messageKey) =>
      setError(
        field,
        { type: "server", message: messageKey },
        { shouldFocus: true }
      ),
  }

  const plateId = `${id}-plate`
  const configurationFieldId = `${id}-configuration`
  const plateError = errorOf("patente")
  const configurationError = errorOf("configuracionId")

  return (
    <form
      id={id}
      noValidate
      onSubmit={handleSubmit((values) => onSubmit(values, helpers))}
    >
      <fieldset disabled={disabled} className="flex flex-col gap-6">
        <div className="grid gap-x-4 gap-y-5 sm:grid-cols-2">
          <Field data-invalid={plateError ? true : undefined}>
            <FieldLabel htmlFor={plateId}>
              {t("form.plate.label")}
              <RequiredMark />
            </FieldLabel>
            <Input
              id={plateId}
              placeholder={t("form.plate.placeholder")}
              autoComplete="off"
              autoCapitalize="characters"
              spellCheck={false}
              maxLength={15}
              className="uppercase placeholder:normal-case"
              aria-invalid={plateError ? true : undefined}
              aria-describedby={`${plateId}-description${plateError ? ` ${plateId}-error` : ""}`}
              aria-required
              {...register("patente")}
            />
            <FieldDescription id={`${plateId}-description`}>
              {t("form.plate.description")}
            </FieldDescription>
            {plateError ? (
              <FieldError id={`${plateId}-error`}>{plateError}</FieldError>
            ) : null}
          </Field>

          <Controller
            control={control}
            name="configuracionId"
            render={({ field }) => (
              <Field data-invalid={configurationError ? true : undefined}>
                <FieldLabel htmlFor={configurationFieldId}>
                  {t("form.configuration.label")}
                  <RequiredMark />
                </FieldLabel>
                <Select
                  id={configurationFieldId}
                  items={options}
                  value={field.value || null}
                  onValueChange={(next) => field.onChange(next ?? "")}
                  disabled={configurations.isPending || configurations.isError}
                >
                  <SelectTrigger
                    ref={field.ref}
                    className="w-full"
                    onBlur={field.onBlur}
                    aria-invalid={configurationError ? true : undefined}
                    aria-describedby={
                      configurationError
                        ? `${configurationFieldId}-error`
                        : undefined
                    }
                    aria-required
                  >
                    <SelectValue
                      placeholder={
                        configurations.isPending
                          ? t("form.configuration.loading")
                          : t("form.configuration.placeholder")
                      }
                    />
                  </SelectTrigger>
                  {/* Puede ser más ancho que el trigger: los nombres de las
                      configuraciones son largos y no se cortan. */}
                  <SelectContent className="w-auto max-w-(--available-width) min-w-(--anchor-width)">
                    {options.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {configurations.isError ? (
                  <FieldDescription>
                    {t("form.configuration.error")}
                  </FieldDescription>
                ) : null}
                {configurationError ? (
                  <FieldError id={`${configurationFieldId}-error`}>
                    {configurationError}
                  </FieldError>
                ) : null}
              </Field>
            )}
          />
        </div>

        {selected ? (
          <AxleLayoutPreview configuration={selected} />
        ) : (
          <p className="rounded-3xl border border-dashed px-4 py-6 text-center text-sm text-muted-foreground">
            {t("preview.empty")}
          </p>
        )}
      </fieldset>
    </form>
  )
}

function RequiredMark() {
  return <span aria-hidden>*</span>
}
