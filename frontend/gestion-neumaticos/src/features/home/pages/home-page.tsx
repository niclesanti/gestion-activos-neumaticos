import { LayoutDashboardIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { EmptyState } from "@/components/common/empty-state"
import { Separator } from "@/components/ui/separator"
import { WelcomeHeader } from "@/features/home/components/welcome-header"

export function HomePage() {
  const { t } = useTranslation("home")

  // TODO: indicadores del tablero de control. Al implementarlos, quitar el
  // placeholder (<EmptyState>) y sus textos `dashboard.empty` en i18n.
  return (
    <div data-slot="home-page" className="flex flex-col gap-6">
      <WelcomeHeader />
      <Separator />
      <section className="flex flex-col gap-1">
        <h2 className="font-heading text-lg font-semibold">
          {t("dashboard.title")}
        </h2>
        <p className="text-sm text-muted-foreground">
          {t("dashboard.subtitle")}
        </p>
      </section>
      <EmptyState
        icon={LayoutDashboardIcon}
        title={t("dashboard.empty.title")}
        description={t("dashboard.empty.description")}
      />
    </div>
  )
}
