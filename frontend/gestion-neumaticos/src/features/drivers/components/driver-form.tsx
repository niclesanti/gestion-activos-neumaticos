import type * as React from "react"

import { zodResolver } from "@hookform/resolvers/zod"
import type { ParseKeys } from "i18next"
import { useForm } from "react-hook-form"
import { useTranslation } from "react-i18next"

import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import {
  driverSchema,
  type DriverFormInput,
  type DriverFormValues,
} from "@/features/drivers/schemas/driver-schema"

/** Los mensajes de zod son claves del namespace `drivers` (ver driver-schema). */
type DriversKey = ParseKeys<"drivers">

type FieldName = keyof DriverFormInput

/** Para marcar en un campo un error que devolvió el backend (ej. 409). */
export type DriverFormHelpers = {
  setServerError: (field: FieldName, messageKey: DriversKey) => void
}

type DriverFormProps = {
  /** Id del `<form>`: el botón de envío puede vivir afuera (ej. en el footer del dialog). */
  id: string
  defaultValues?: Partial<DriverFormInput>
  disabled?: boolean
  onSubmit: (values: DriverFormValues, helpers: DriverFormHelpers) => void
}

const EMPTY_DRIVER: DriverFormInput = {
  dni: "",
  nombre: "",
  apellido: "",
}

/**
 * Formulario de un conductor, pensado para el alta y la futura modificación.
 * Valida exactamente lo mismo que `ConductorRequestDTO` (ver driver-schema) y
 * entrega el cuerpo listo para la API.
 */
export function DriverForm({
  id,
  defaultValues,
  disabled = false,
  onSubmit,
}: DriverFormProps) {
  const { t } = useTranslation("drivers")

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<DriverFormInput, unknown, DriverFormValues>({
    resolver: zodResolver(driverSchema),
    defaultValues: { ...EMPTY_DRIVER, ...defaultValues },
  })

  const errorOf = (name: FieldName) => {
    const message = errors[name]?.message
    return message ? t(message as DriversKey) : undefined
  }

  const helpers: DriverFormHelpers = {
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
      <fieldset
        disabled={disabled}
        className="grid gap-x-4 gap-y-5 sm:grid-cols-2"
      >
        <TextField
          id={`${id}-first-name`}
          label={t("form.firstName.label")}
          error={errorOf("nombre")}
          required
        >
          <Input
            id={`${id}-first-name`}
            placeholder={t("form.firstName.placeholder")}
            autoComplete="off"
            maxLength={120}
            {...ariaFor(`${id}-first-name`, errorOf("nombre"), true)}
            {...register("nombre")}
          />
        </TextField>

        <TextField
          id={`${id}-last-name`}
          label={t("form.lastName.label")}
          error={errorOf("apellido")}
          required
        >
          <Input
            id={`${id}-last-name`}
            placeholder={t("form.lastName.placeholder")}
            autoComplete="off"
            maxLength={120}
            {...ariaFor(`${id}-last-name`, errorOf("apellido"), true)}
            {...register("apellido")}
          />
        </TextField>

        <TextField
          id={`${id}-dni`}
          className="sm:col-span-2"
          label={t("form.dni.label")}
          description={t("form.dni.description")}
          error={errorOf("dni")}
        >
          <Input
            id={`${id}-dni`}
            placeholder={t("form.dni.placeholder")}
            autoComplete="off"
            inputMode="numeric"
            spellCheck={false}
            maxLength={20}
            {...ariaFor(`${id}-dni`, errorOf("dni"))}
            {...register("dni")}
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

/** Hereda el color del label: neutro, y rojo solo cuando el campo tiene error. */
function RequiredMark() {
  return <span aria-hidden>*</span>
}
