import { Controller, useFormContext, useWatch } from "react-hook-form"
import { useTranslation } from "react-i18next"

import {
  SelectField,
  TextField,
  type SelectOption,
} from "@/components/common/form-fields"
import { FieldDescription, FieldLegend, FieldSet } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import {
  hasAddress,
  type AddressFormInput,
  type AddressMessageKey,
} from "@/lib/address"
import { useProvinces } from "@/lib/api/provinces"
import { ariaFor } from "@/lib/aria"

type AddressFieldsetProps = {
  /** Prefijo de los ids de los campos (normalmente, el id del `<form>`). */
  idPrefix: string
}

/**
 * Sección "Domicilio" (opcional) de un formulario: calle, altura, piso,
 * departamento, provincia, localidad y código postal. Debe renderizarse dentro
 * de un `FormProvider` cuyo formulario incluya `addressShape`. Los datos
 * principales pasan a ser obligatorios apenas se empieza a cargar uno.
 */
export function AddressFieldset({ idPrefix }: AddressFieldsetProps) {
  const { t } = useTranslation("address")
  const provinces = useProvinces()
  const {
    control,
    register,
    formState: { errors },
  } = useFormContext<AddressFormInput>()

  const required = hasAddress(useWatch({ control }))

  const provinceOptions: SelectOption[] = (provinces.data ?? []).map(
    (province) => ({ value: String(province.id), label: province.nombre })
  )

  const errorOf = (name: keyof AddressFormInput) => {
    const message = errors[name]?.message
    return message ? t(message as AddressMessageKey) : undefined
  }

  return (
    <FieldSet className="gap-0">
      <FieldLegend>{t("legend")}</FieldLegend>
      <FieldDescription className="-mt-2 mb-4">
        {t("description")}
      </FieldDescription>
      <div className="grid gap-x-4 gap-y-5 sm:grid-cols-6">
        <TextField
          id={`${idPrefix}-street`}
          className="sm:col-span-4"
          label={t("street.label")}
          error={errorOf("calle")}
          required={required}
        >
          <Input
            id={`${idPrefix}-street`}
            placeholder={t("street.placeholder")}
            autoComplete="off"
            maxLength={100}
            {...ariaFor(`${idPrefix}-street`, errorOf("calle"), required)}
            {...register("calle")}
          />
        </TextField>

        <TextField
          id={`${idPrefix}-number`}
          className="sm:col-span-2"
          label={t("number.label")}
          error={errorOf("altura")}
          required={required}
        >
          <Input
            id={`${idPrefix}-number`}
            placeholder={t("number.placeholder")}
            autoComplete="off"
            maxLength={10}
            {...ariaFor(`${idPrefix}-number`, errorOf("altura"), required)}
            {...register("altura")}
          />
        </TextField>

        <TextField
          id={`${idPrefix}-floor`}
          className="sm:col-span-3"
          label={t("floor.label")}
          error={errorOf("piso")}
        >
          <Input
            id={`${idPrefix}-floor`}
            placeholder={t("floor.placeholder")}
            autoComplete="off"
            maxLength={10}
            {...ariaFor(`${idPrefix}-floor`, errorOf("piso"))}
            {...register("piso")}
          />
        </TextField>

        <TextField
          id={`${idPrefix}-apartment`}
          className="sm:col-span-3"
          label={t("apartment.label")}
          error={errorOf("departamento")}
        >
          <Input
            id={`${idPrefix}-apartment`}
            placeholder={t("apartment.placeholder")}
            autoComplete="off"
            maxLength={10}
            {...ariaFor(`${idPrefix}-apartment`, errorOf("departamento"))}
            {...register("departamento")}
          />
        </TextField>

        <Controller
          control={control}
          name="provinciaId"
          render={({ field }) => (
            <SelectField
              id={`${idPrefix}-province`}
              className="sm:col-span-3"
              label={t("province.label")}
              placeholder={
                provinces.isPending
                  ? t("province.loading")
                  : t("province.placeholder")
              }
              description={provinces.isError ? t("province.error") : undefined}
              options={provinceOptions}
              value={field.value}
              onChange={field.onChange}
              onBlur={field.onBlur}
              error={errorOf("provinciaId")}
              disabled={provinces.isPending || provinces.isError}
              required={required}
            />
          )}
        />

        <TextField
          id={`${idPrefix}-city`}
          className="sm:col-span-3"
          label={t("city.label")}
          description={t("city.description")}
          error={errorOf("localidad")}
          required={required}
        >
          <Input
            id={`${idPrefix}-city`}
            placeholder={t("city.placeholder")}
            autoComplete="off"
            maxLength={100}
            {...ariaFor(`${idPrefix}-city`, errorOf("localidad"), required)}
            {...register("localidad")}
          />
        </TextField>

        <TextField
          id={`${idPrefix}-postal-code`}
          className="sm:col-span-3"
          label={t("postalCode.label")}
          description={t("postalCode.description")}
          error={errorOf("codigoPostal")}
        >
          <Input
            id={`${idPrefix}-postal-code`}
            placeholder={t("postalCode.placeholder")}
            autoComplete="off"
            spellCheck={false}
            maxLength={8}
            {...ariaFor(`${idPrefix}-postal-code`, errorOf("codigoPostal"))}
            {...register("codigoPostal")}
          />
        </TextField>
      </div>
    </FieldSet>
  )
}
