import * as React from "react"

import { Loader2Icon } from "lucide-react"
import { useTranslation } from "react-i18next"

import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import type { UnitSlotDetail } from "@/features/transport-units/api/transport-units-api"
import { useUnassignTire } from "@/features/transport-units/hooks/use-unassign-tire"
import { getHttpStatus } from "@/lib/api/client"
import { notify } from "@/lib/toast"

/**
 * Confirmación del desmontaje (HU-32): nombra el neumático y la posición y no
 * ejecuta nada hasta confirmar. El foco inicial queda en Cancelar.
 */
export function UnassignTireDialog({
  unitPublicId,
  plate,
  slot,
  open,
  onOpenChange,
  onRefreshUnit,
}: {
  unitPublicId: string
  plate: string
  /** Posición a desmontar; se conserva al cerrar para la animación. */
  slot: UnitSlotDetail | null
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Vuelve a leer la ficha (tras un 409 o un 404). */
  onRefreshUnit: () => Promise<unknown>
}) {
  const { t } = useTranslation("transport-units")
  const unassign = useUnassignTire(unitPublicId)
  const cancelRef = React.useRef<HTMLButtonElement>(null)
  const serial = slot?.neumatico?.numeroSerie ?? t("slot.notRegistered")

  function confirm() {
    if (!slot) return
    unassign.mutate(slot.codigo, {
      onSuccess: () => {
        notify.success({
          titleKey: "transport-units:unmount.feedback.done",
          descriptionKey: "transport-units:unmount.feedback.description",
          values: { serial, plate, position: slot.codigo },
        })
        onOpenChange(false)
      },
      onError: (error) => {
        const status = getHttpStatus(error)
        if (status === 409 || status === 404) {
          void onRefreshUnit()
          onOpenChange(false)
          notify.error({
            titleKey:
              status === 409
                ? "transport-units:unmount.errors.alreadyFree"
                : "transport-units:unmount.errors.notFound",
          })
        } else if (status === 403) {
          notify.error({ titleKey: "transport-units:unmount.errors.forbidden" })
        } else {
          notify.error({
            titleKey: "transport-units:unmount.errors.unexpected",
          })
        }
      },
    })
  }

  return (
    <AlertDialog
      open={open}
      onOpenChange={(next) => {
        if (!unassign.isPending) onOpenChange(next)
      }}
    >
      <AlertDialogContent initialFocus={cancelRef}>
        <AlertDialogHeader>
          <AlertDialogTitle>{t("unmount.title")}</AlertDialogTitle>
          <AlertDialogDescription>
            {t("unmount.description", {
              serial,
              position: slot?.codigo ?? "",
            })}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel ref={cancelRef} disabled={unassign.isPending}>
            {t("unmount.cancel")}
          </AlertDialogCancel>
          <AlertDialogAction
            variant="destructive"
            disabled={unassign.isPending}
            onClick={confirm}
          >
            {unassign.isPending ? (
              <Loader2Icon className="animate-spin" />
            ) : null}
            {unassign.isPending
              ? t("unmount.submitting")
              : t("unmount.confirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
