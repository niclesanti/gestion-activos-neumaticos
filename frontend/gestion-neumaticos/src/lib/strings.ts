/** Recorta los extremos; un texto vacío o en blanco equivale a no tenerlo (`null`). */
export function emptyToNull(value: string) {
  const trimmed = value.trim()
  return trimmed === "" ? null : trimmed
}
