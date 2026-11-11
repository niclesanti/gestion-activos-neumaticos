import { ArrowDownIcon, ArrowUpDownIcon, ArrowUpIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { TableHead } from "@/components/ui/table"

/**
 * Encabezado de columna ordenable: muestra el sentido del orden si es la
 * columna activa y avisa con `onToggle` cuando se hace clic.
 */
export function SortableTableHead<F extends string>({
  field,
  label,
  activeField,
  direction,
  onToggle,
}: {
  field: F
  label: string
  activeField: F
  direction: "asc" | "desc"
  onToggle: (field: F) => void
}) {
  const active = activeField === field
  const Icon = !active
    ? ArrowUpDownIcon
    : direction === "asc"
      ? ArrowUpIcon
      : ArrowDownIcon

  return (
    <TableHead
      aria-sort={
        active ? (direction === "asc" ? "ascending" : "descending") : "none"
      }
    >
      <Button
        type="button"
        variant="ghost"
        size="sm"
        className="-ml-2.5"
        onClick={() => onToggle(field)}
      >
        {label}
        <Icon data-icon="inline-end" />
      </Button>
    </TableHead>
  )
}
