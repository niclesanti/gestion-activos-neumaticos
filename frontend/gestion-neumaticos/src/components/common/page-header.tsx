import { cn } from "@/lib/utils"

/**
 * Encabezado estándar de página: título, bajada y, opcionalmente, las acciones
 * principales de la sección (a la derecha en escritorio, debajo en mobile).
 * Lo usan todas las secciones.
 */
export function PageHeader({
  title,
  description,
  actions,
  className,
  ...props
}: React.ComponentProps<"header"> & {
  title: string
  description?: string
  actions?: React.ReactNode
}) {
  return (
    <header
      data-slot="page-header"
      className={cn(
        "flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between",
        className
      )}
      {...props}
    >
      <div className="flex flex-col gap-1">
        <h1 className="font-heading text-2xl font-semibold">{title}</h1>
        {description ? (
          <p className="text-sm text-muted-foreground">{description}</p>
        ) : null}
      </div>
      {actions ? (
        <div className="flex shrink-0 items-center gap-2">{actions}</div>
      ) : null}
    </header>
  )
}
