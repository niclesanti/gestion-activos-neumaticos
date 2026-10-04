import { cn } from "@/lib/utils"

/** Encabezado estándar de página: título y bajada. Lo usan todas las secciones. */
export function PageHeader({
  title,
  description,
  className,
  ...props
}: React.ComponentProps<"header"> & {
  title: string
  description?: string
}) {
  return (
    <header
      data-slot="page-header"
      className={cn("flex flex-col gap-1", className)}
      {...props}
    >
      <h1 className="font-heading text-2xl font-semibold">{title}</h1>
      {description ? (
        <p className="text-sm text-muted-foreground">{description}</p>
      ) : null}
    </header>
  )
}
