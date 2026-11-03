import {
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from "@/components/ui/empty"
import { cn } from "@/lib/utils"

/**
 * Estado vacío estándar (ícono + título + descripción) de las secciones y
 * catálogos que todavía no tienen contenido. Mismo diseño que el del home.
 */
export function EmptyState({
  icon: Icon,
  title,
  description,
  className,
  ...props
}: React.ComponentProps<typeof Empty> & {
  icon: React.ElementType
  title: string
  description: string
}) {
  return (
    <Empty
      data-slot="empty-state"
      className={cn("border", className)}
      {...props}
    >
      <EmptyHeader>
        <EmptyMedia variant="icon">
          <Icon />
        </EmptyMedia>
        <EmptyTitle>{title}</EmptyTitle>
        <EmptyDescription>{description}</EmptyDescription>
      </EmptyHeader>
    </Empty>
  )
}
