import { zodResolver } from "@hookform/resolvers/zod"
import type { ParseKeys } from "i18next"
import { FormProvider, useForm } from "react-hook-form"
import { useTranslation } from "react-i18next"

import { AddressFieldset } from "@/components/common/address-fieldset"
import { TextField } from "@/components/common/form-fields"
import { FieldDescription, FieldLegend, FieldSet } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Separator } from "@/components/ui/separator"
import {
  supplierSchema,
  type SupplierFormInput,
  type SupplierFormValues,
} from "@/features/suppliers/schemas/supplier-schema"
import type { AddressMessageKey } from "@/lib/address"
import { ariaFor } from "@/lib/aria"

/** Los mensajes de zod son claves del namespace `suppliers` (ver supplier-schema). */
type SuppliersKey = ParseKeys<"suppliers">

type FieldName = keyof SupplierFormInput

/**
 * Para marcar en un campo un error que devolvió el backend (ej. 409). Los
 * campos del domicilio llevan una clave del namespace `address`.
 */
export type SupplierFormHelpers = {
  setServerError: (
    field: FieldName,
    messageKey: SuppliersKey | AddressMessageKey
  ) => void
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

  const form = useForm<SupplierFormInput, unknown, SupplierFormValues>({
    resolver: zodResolver(supplierSchema),
    defaultValues: { ...EMPTY_SUPPLIER, ...defaultValues },
  })
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = form

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

          <AddressFieldset idPrefix={id} />
        </fieldset>
      </form>
    </FormProvider>
  )
}
