import { useTranslation } from "react-i18next"

import { PageHeader } from "@/components/common/page-header"

export function HomePage() {
  const { t } = useTranslation("home")

  // TODO: indicadores del tablero de control.
  return (
    <div data-slot="home-page" className="flex flex-col gap-6">
      <PageHeader title={t("title")} description={t("subtitle")} />
    </div>
  )
}
