/** Atributos de accesibilidad de un campo de formulario según su error y si es obligatorio. */
export function ariaFor(
  id: string,
  error: string | undefined,
  required = false
) {
  return {
    "aria-invalid": error ? true : undefined,
    "aria-describedby": error ? `${id}-error` : undefined,
    "aria-required": required || undefined,
  }
}
