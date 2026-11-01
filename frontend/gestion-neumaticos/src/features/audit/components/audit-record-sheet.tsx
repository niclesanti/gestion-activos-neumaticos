import { useTranslation } from "react-i18next"

import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet"
import type { Auditor, AuditRecord } from "@/features/audit/api/audit-api"
import { AuditStatusBadge } from "@/features/audit/components/audit-status-badge"
import { AuditorLabel } from "@/features/audit/components/auditor-label"
import { formatAbsolute } from "@/features/audit/lib/format-date"

type TimelineStep = {
  key: "created" | "modified" | "deleted"
  at: string
  by: Auditor | null
}

/** Pasos que tiene el registro: alta, última modificación (si hubo) y baja (si hubo). */
function getSteps(record: AuditRecord): TimelineStep[] {
  const steps: TimelineStep[] = [
    { key: "created", at: record.creadoEn, by: record.creadoPor },
  ]
  if (record.modificadoEn !== record.creadoEn) {
    steps.push({
      key: "modified",
      at: record.modificadoEn,
      by: record.modificadoPor,
    })
  }
  if (record.dadoDeBajaEn) {
    steps.push({
      key: "deleted",
      at: record.dadoDeBajaEn,
      by: record.dadoDeBajaPor,
    })
  }
  return steps
}

/** Detalle de un registro: línea de tiempo alta → modificación → baja, solo lectura. */
export function AuditRecordSheet({
  record,
  onClose,
}: {
  record: AuditRecord | null
  onClose: () => void
}) {
  const { t, i18n } = useTranslation("audit")

  return (
    <Sheet open={record !== null} onOpenChange={(open) => !open && onClose()}>
      <SheetContent>
        {record ? (
          <>
            <SheetHeader>
              <SheetTitle className="break-words">
                {record.descripcion}
              </SheetTitle>
              <SheetDescription>{t("detail.description")}</SheetDescription>
              <div>
                <AuditStatusBadge status={record.estado} />
              </div>
            </SheetHeader>

            <ol
              data-slot="audit-timeline"
              className="flex flex-col gap-6 px-4 pb-6"
            >
              {getSteps(record).map((step) => (
                <li
                  key={step.key}
                  className="relative flex flex-col gap-1 border-l pl-4"
                >
                  <span
                    aria-hidden
                    className="absolute top-1.5 -left-1 size-2 rounded-full bg-primary"
                  />
                  <span className="text-sm font-medium">
                    {t(`detail.${step.key}`)}
                  </span>
                  <time
                    dateTime={step.at}
                    className="text-sm text-muted-foreground"
                  >
                    {formatAbsolute(step.at, i18n.resolvedLanguage)}
                  </time>
                  <div className="text-sm">
                    <AuditorLabel auditor={step.by} />
                  </div>
                </li>
              ))}
              {record.modificadoEn === record.creadoEn ? (
                <li className="pl-4 text-sm text-muted-foreground">
                  {t("unmodified")}
                </li>
              ) : null}
            </ol>
          </>
        ) : null}
      </SheetContent>
    </Sheet>
  )
}
