import type * as React from "react"

import {
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
} from "@/components/ui/field"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { ariaFor } from "@/lib/aria"

export type SelectOption = { value: string; label: string }

/** Hereda el color del label: neutro, y rojo solo cuando el campo tiene error. */
function RequiredMark() {
  return <span aria-hidden>*</span>
}

/** Campo de texto con label, descripción y error; los textos llegan ya traducidos. */
export function TextField({
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

/** Select con label, descripción y error; los textos llegan ya traducidos. */
export function SelectField({
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
  options: SelectOption[]
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
