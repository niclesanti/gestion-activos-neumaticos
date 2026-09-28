import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"
import { Button } from "@/components/ui/button"
import { notify } from "@/lib/toast"

/**
 * Showcase de todos los tipos de notificación que documenta shadcn, para poder
 * revisar estilo, tema claro/oscuro y traducción en vivo en el navegador.
 *
 * Sólo se monta en desarrollo (ver `src/app/router.tsx`): es descartable y no
 * forma parte del dominio, de ahí que viva en un feature `dev`.
 */
const DEMOS = [
  {
    id: "default",
    labelKey: "toasts.trigger.default",
    run: () => notify.show({ titleKey: "dev:toasts.sample.default.title" }),
  },
  {
    id: "success",
    labelKey: "toasts.trigger.success",
    run: () => notify.success({ titleKey: "dev:toasts.sample.success.title" }),
  },
  {
    id: "info",
    labelKey: "toasts.trigger.info",
    run: () => notify.info({ titleKey: "dev:toasts.sample.info.title" }),
  },
  {
    id: "warning",
    labelKey: "toasts.trigger.warning",
    run: () =>
      notify.warning({
        titleKey: "dev:toasts.sample.warning.title",
        descriptionKey: "dev:toasts.sample.warning.description",
        values: { code: "NEU-0421" },
      }),
  },
  {
    id: "error",
    labelKey: "toasts.trigger.error",
    run: () =>
      notify.error({
        titleKey: "dev:toasts.sample.error.title",
        descriptionKey: "dev:toasts.sample.error.description",
      }),
  },
  {
    id: "loading",
    labelKey: "toasts.trigger.loading",
    run: () =>
      notify.loading({
        titleKey: "dev:toasts.sample.loading.title",
        descriptionKey: "dev:toasts.sample.loading.description",
      }),
  },
  {
    id: "description",
    labelKey: "toasts.trigger.description",
    run: () =>
      notify.show({
        titleKey: "dev:toasts.sample.default.title",
        descriptionKey: "dev:toasts.sample.default.description",
        values: { code: "NEU-0421" },
      }),
  },
  {
    id: "action",
    labelKey: "toasts.trigger.action",
    run: () => {
      const id = notify.show({
        titleKey: "dev:toasts.sample.action.title",
        actionLabelKey: "dev:toasts.sample.action.label",
        onAction: () => {
          notify.close(id)
          notify.success({ titleKey: "dev:toasts.sample.action.undone" })
        },
      })
    },
  },
  {
    id: "promise",
    labelKey: "toasts.trigger.promise",
    run: () => {
      const saving = new Promise<void>((resolve) =>
        window.setTimeout(resolve, 2000)
      )

      void notify.promise(saving, {
        loading: { titleKey: "dev:toasts.sample.promise.loading" },
        success: { titleKey: "dev:toasts.sample.promise.success" },
        error: { titleKey: "dev:toasts.sample.promise.error" },
      })
    },
  },
  {
    id: "stacked",
    labelKey: "toasts.trigger.stacked",
    run: () => {
      for (const index of [1, 2, 3]) {
        notify.show({
          titleKey: "dev:toasts.sample.stacked.title",
          values: { index },
        })
      }
    },
  },
] as const

export function ToastShowcasePage() {
  const { t } = useTranslation("dev")

  return (
    <div data-slot="toast-showcase-page" className="flex flex-col gap-6">
      <PageHeader
        title={t("toasts.title")}
        description={t("toasts.subtitle")}
      />

      <div className="flex flex-wrap gap-2">
        {DEMOS.map((demo) => (
          <Button key={demo.id} variant="outline" onClick={demo.run}>
            {t(demo.labelKey)}
          </Button>
        ))}
      </div>
    </div>
  )
}
