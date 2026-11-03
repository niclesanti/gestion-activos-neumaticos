import { zodResolver } from "@hookform/resolvers/zod"
import type { ParseKeys } from "i18next"
import { FormProvider, useForm } from "react-hook-form"
import { useTranslation } from "react-i18next"

import { AddressFieldset } from "@/components/common/address-fieldset"
import { TextField } from "@/components/common/form-fields"
import { Input } from "@/components/ui/input"
import { Separator } from "@/components/ui/separator"
import {
  storageCenterSchema,
  type StorageCenterFormInput,
  type StorageCenterFormValues,
} from "@/features/storage-centers/schemas/storage-center-schema"
import type { AddressMessageKey } from "@/lib/address"
import { ariaFor } from "@/lib/aria"

/** Los mensajes de zod son claves del namespace `storage-centers` (ver storage-center-schema). */
type StorageCentersKey = ParseKeys<"storage-centers">

type FieldName = keyof StorageCenterFormInput

/**
 * Para marcar en un campo un error que devolvió el backend (ej. 404 de la
 * provincia). Los campos del domicilio llevan una clave del namespace `address`.
 */
export type StorageCenterFormHelpers = {
  setServerError: (
    field: FieldName,
    messageKey: StorageCentersKey | AddressMessageKey
  ) => void
}

type StorageCenterFormProps = {
  /** Id del `<form>`: el botón de envío puede vivir afuera (ej. en el footer del dialog). */
  id: string
  defaultValues?: Partial<StorageCenterFormInput>
  disabled?: boolean
  onSubmit: (
    values: StorageCenterFormValues,
    helpers: StorageCenterFormHelpers
  ) => void
}

const EMPTY_STORAGE_CENTER: StorageCenterFormInput = {
  nombre: "",
  calle: "",
  altura: "",
  piso: "",
  departamento: "",
  provinciaId: "",
  localidad: "",
  codigoPostal: "",
}

/**
 * Formulario de un centro de almacenamiento. Valida exactamente lo mismo que
 * `CentroAlmacenamientoRequestDTO` (ver storage-center-schema) y entrega el
 * cuerpo listo para la API. El domicilio es obligatorio.
 */
export function StorageCenterForm({
  id,
  defaultValues,
  disabled = false,
  onSubmit,
}: StorageCenterFormProps) {
  const { t } = useTranslation("storage-centers")

  const form = useForm<
    StorageCenterFormInput,
    unknown,
    StorageCenterFormValues
  >({
    resolver: zodResolver(storageCenterSchema),
    defaultValues: { ...EMPTY_STORAGE_CENTER, ...defaultValues },
  })
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = form

  const errorOf = (name: FieldName) => {
    const message = errors[name]?.message
    return message ? t(message as StorageCentersKey) : undefined
  }

  const helpers: StorageCenterFormHelpers = {
    setServerError: (field, messageKey) =>
      setError(
        field,
        { type: "server", message: messageKey },
        { shouldFocus: true }
      ),
  }

  return (
    <FormProvider {...form}>
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

          <AddressFieldset idPrefix={id} required />
        </fieldset>
      </form>
    </FormProvider>
  )
}
