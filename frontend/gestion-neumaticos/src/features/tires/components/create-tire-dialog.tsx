import * as React from "react"

import { Loader2Icon, PlusIcon } from "lucide-react"
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
import {
  TireForm,
  type TireFormHelpers,
} from "@/features/tires/components/tire-form"
import { useRegisterTire } from "@/features/tires/hooks/use-register-tire"
import type { TireFormValues } from "@/features/tires/schemas/tire-schema"
import { getHttpStatus } from "@/lib/api/client"
import { notify } from "@/lib/toast"

const FORM_ID = "create-tire-form"

type FeedbackKey =
  | "feedback.duplicateSerial"
  | "feedback.notFound"
  | "feedback.forbidden"
  | "feedback.invalid"
  | "feedback.unexpected"

function toFeedbackKey(error: unknown): FeedbackKey {
  switch (getHttpStatus(error)) {
    case 409:
      return "feedback.duplicateSerial"
    case 404:
      return "feedback.notFound"
    case 403:
      return "feedback.forbidden"
    case 400:
      return "feedback.invalid"
    default:
      return "feedback.unexpected"
  }
}

type PendingRegistration = {
  values: TireFormValues
  helpers: TireFormHelpers
}

/**
 * Alta de un neumático (HU-11): el formulario valida y, antes de enviar, se
 * pide confirmación porque el número de serie no se puede cambiar después.
 */
export function CreateTireDialog() {
  const { t } = useTranslation("tires")
  const registerTire = useRegisterTire()
  const [open, setOpen] = React.useState(false)
  const [pending, setPending] = React.useState<PendingRegistration | null>(null)
  const [error, setError] = React.useState<FeedbackKey | null>(null)

  const changeOpen = (next: boolean) => {
    // No se cierra a mitad del envío: el resultado se perdería de vista.
    if (registerTire.isPending) return
    setOpen(next)
    if (!next) setError(null)
  }

  const confirm = () => {
    if (!pending) return
    const { values, helpers } = pending
    setError(null)
    registerTire.mutate(values, {
      onSuccess: () => {
        setPending(null)
        setOpen(false)
        notify.success({
          titleKey: "tires:feedback.created",
          descriptionKey: "tires:feedback.createdDescription",
        })
      },
      onError: (mutationError) => {
        setPending(null)
        const key = toFeedbackKey(mutationError)
        if (key === "feedback.duplicateSerial") {
          helpers.setServerError("numeroSerie", key)
        } else {
          setError(key)
        }
        notify.error({ titleKey: `tires:${key}` })
      },
    })
  }

  const isSubmitting = registerTire.isPending

  return (
    <>
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
            <DialogDescription>
              {t("dialog.createDescription")}
            </DialogDescription>
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
              <TireForm
                id={FORM_ID}
                mode="create"
                disabled={isSubmitting}
                onSubmit={(values, helpers) => setPending({ values, helpers })}
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

      <AlertDialog
        open={pending !== null}
        onOpenChange={(next) => {
          if (!next && !isSubmitting) setPending(null)
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t("confirm.title")}</AlertDialogTitle>
            <AlertDialogDescription>
              {pending?.values.numeroSerie
                ? t("confirm.withSerial", {
                    serial: pending.values.numeroSerie,
                  })
                : t("confirm.withoutSerial")}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isSubmitting}>
              {t("confirm.cancel")}
            </AlertDialogCancel>
            <AlertDialogAction disabled={isSubmitting} onClick={confirm}>
              {isSubmitting ? <Loader2Icon className="animate-spin" /> : null}
              {t("confirm.confirm")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
