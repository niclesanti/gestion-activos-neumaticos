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
import { DriverForm } from "@/features/drivers/components/driver-form"
import { useRegisterDriver } from "@/features/drivers/hooks/use-register-driver"
import { getHttpStatus } from "@/lib/api/client"
import { notify } from "@/lib/toast"

const FORM_ID = "create-driver-form"

type FeedbackKey =
  | "feedback.duplicateDni"
  | "feedback.forbidden"
  | "feedback.invalid"
  | "feedback.unexpected"

function toFeedbackKey(error: unknown): FeedbackKey {
  switch (getHttpStatus(error)) {
    case 409:
      return "feedback.duplicateDni"
    case 403:
      return "feedback.forbidden"
    case 400:
      return "feedback.invalid"
    default:
      return "feedback.unexpected"
  }
}

/** Alta de un conductor: el formulario valida y envía directamente. */
export function CreateDriverDialog() {
  const { t } = useTranslation("drivers")
  const registerDriver = useRegisterDriver()
  const [open, setOpen] = React.useState(false)
  const [error, setError] = React.useState<FeedbackKey | null>(null)

  const changeOpen = (next: boolean) => {
    // No se cierra a mitad del envío: el resultado se perdería de vista.
    if (registerDriver.isPending) return
    setOpen(next)
    if (!next) setError(null)
  }

  const isSubmitting = registerDriver.isPending

  return (
    <Dialog open={open} onOpenChange={changeOpen}>
      <DialogTrigger render={<Button />}>
        <PlusIcon />
        {t("actions.register")}
      </DialogTrigger>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{t("dialog.createTitle")}</DialogTitle>
          <DialogDescription>{t("dialog.createDescription")}</DialogDescription>
        </DialogHeader>
        <div className="flex flex-col gap-5">
          {error ? (
            <p
              role="alert"
              className="rounded-3xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
            >
              {t(error)}
            </p>
          ) : null}
          <DriverForm
            id={FORM_ID}
            disabled={isSubmitting}
            onSubmit={(values, helpers) => {
              setError(null)
              registerDriver.mutate(values, {
                onSuccess: () => {
                  setOpen(false)
                  notify.success({
                    titleKey: "drivers:feedback.created",
                    descriptionKey: "drivers:feedback.createdDescription",
                  })
                },
                onError: (mutationError) => {
                  const key = toFeedbackKey(mutationError)
                  if (key === "feedback.duplicateDni") {
                    helpers.setServerError("dni", key)
                  } else {
                    setError(key)
                  }
                  notify.error({ titleKey: `drivers:${key}` })
                },
              })
            }}
          />
        </div>
        <DialogFooter>
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
