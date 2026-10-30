import * as React from "react"

import { Loader2Icon, PlusIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { ScrollArea } from "@/components/ui/scroll-area"
import { TransportUnitForm } from "@/features/transport-units/components/transport-unit-form"
import { useRegisterTransportUnit } from "@/features/transport-units/hooks/use-register-transport-unit"
import { getHttpStatus } from "@/lib/api/client"
import { notify } from "@/lib/toast"

const FORM_ID = "create-transport-unit-form"

type FeedbackKey =
  | "feedback.duplicatePlate"
  | "feedback.configurationNotFound"
  | "feedback.forbidden"
  | "feedback.invalid"
  | "feedback.unexpected"

function toFeedbackKey(error: unknown): FeedbackKey {
  switch (getHttpStatus(error)) {
    case 409:
      return "feedback.duplicatePlate"
    case 404:
      return "feedback.configurationNotFound"
    case 403:
      return "feedback.forbidden"
    case 400:
      return "feedback.invalid"
    default:
      return "feedback.unexpected"
  }
}

/**
 * Alta de una unidad de transporte (HU-07): patente y configuración; el
 * backend genera todas las posiciones de la configuración.
 */
export function CreateTransportUnitDialog() {
  const { t } = useTranslation("transport-units")
  const registerTransportUnit = useRegisterTransportUnit()
  const [open, setOpen] = React.useState(false)
  const [error, setError] = React.useState<FeedbackKey | null>(null)

  const changeOpen = (next: boolean) => {
    // No se cierra a mitad del envío: el resultado se perdería de vista.
    if (registerTransportUnit.isPending) return
    setOpen(next)
    if (!next) setError(null)
  }

  const isSubmitting = registerTransportUnit.isPending

  return (
    <Dialog open={open} onOpenChange={changeOpen}>
      <DialogTrigger render={<Button />}>
        <PlusIcon />
        {t("actions.register")}
      </DialogTrigger>
      {/* Margen fijo de 2rem arriba y abajo; header y footer quedan fijos y
          solo el formulario scrollea dentro del ScrollArea. */}
      <DialogContent className="flex max-h-[calc(100dvh-4rem)] flex-col gap-0 p-0 sm:max-w-2xl">
        <DialogHeader className="px-6 pt-6 pb-4">
          <DialogTitle>{t("dialog.createTitle")}</DialogTitle>
          <DialogDescription>{t("dialog.createDescription")}</DialogDescription>
        </DialogHeader>
        {/* El Viewport del ScrollArea mide h-full, que no resuelve contra un
            padre con solo max-height: se lo vuelve un ítem flex para que
            tome el alto que le deja el dialog y scrollee adentro. */}
        <ScrollArea className="flex min-h-0 flex-1 flex-col *:data-[slot=scroll-area-viewport]:min-h-0 *:data-[slot=scroll-area-viewport]:flex-1">
          <div className="flex flex-col gap-5 px-6 py-2">
            {error ? (
              <p
                role="alert"
                className="rounded-3xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
              >
                {t(error)}
              </p>
            ) : null}
            <TransportUnitForm
              id={FORM_ID}
              disabled={isSubmitting}
              onSubmit={(values, helpers) => {
                setError(null)
                registerTransportUnit.mutate(values, {
                  onSuccess: (unit) => {
                    setOpen(false)
                    notify.success({
                      titleKey: "transport-units:feedback.created",
                      descriptionKey:
                        "transport-units:feedback.createdDescription",
                      values: {
                        plate: unit.patente,
                        count: unit.slots.length,
                      },
                    })
                  },
                  onError: (mutationError) => {
                    const key = toFeedbackKey(mutationError)
                    if (key === "feedback.duplicatePlate") {
                      helpers.setServerError("patente", key)
                    } else if (key === "feedback.configurationNotFound") {
                      helpers.setServerError("configuracionId", key)
                    } else {
                      setError(key)
                    }
                    notify.error({ titleKey: `transport-units:${key}` })
                  },
                })
              }}
            />
          </div>
        </ScrollArea>
        <DialogFooter className="px-6 pt-4 pb-6">
          <DialogClose
            render={<Button variant="outline" disabled={isSubmitting} />}
          >
            {t("dialog.cancel")}
          </DialogClose>
          <Button type="submit" form={FORM_ID} disabled={isSubmitting}>
            {isSubmitting ? <Loader2Icon className="animate-spin" /> : null}
            {isSubmitting ? t("dialog.submitting") : t("dialog.submit")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
