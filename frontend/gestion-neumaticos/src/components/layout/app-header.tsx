import { useTranslation } from "react-i18next"

import { NavBreadcrumb } from "@/components/layout/nav-breadcrumb"
import { Separator } from "@/components/ui/separator"
import { SidebarTrigger } from "@/components/ui/sidebar"

export function AppHeader() {
  const { t } = useTranslation()

  return (
    <header className="flex h-16 shrink-0 items-center gap-2 border-b border-border bg-background px-4">
      <SidebarTrigger aria-label={t("sidebar.toggle")} className="-ml-1" />
      <Separator orientation="vertical" className="mr-1 h-4" />
      <NavBreadcrumb />
    </header>
  )
}
