import { useTranslation } from "react-i18next"

import { AppLogo } from "@/components/brand/app-logo"
import { LanguageToggle } from "@/components/language-toggle"
import { ThemeToggle } from "@/components/theme-toggle"
import { LoginCard } from "@/features/auth/components/LoginCard"

export function LoginPage() {
  const { t } = useTranslation()

  return (
    <div className="flex min-h-svh flex-col bg-background">
      <header className="flex items-center justify-between px-6 py-4">
        <AppLogo />
        <div className="flex items-center gap-1">
          <LanguageToggle />
          <ThemeToggle />
        </div>
      </header>
      <main className="flex flex-1 items-center justify-center px-6 py-8">
        <LoginCard />
      </main>
      <footer className="px-6 py-6 text-center text-xs text-muted-foreground">
        {t("footer.copyright", {
          year: new Date().getFullYear(),
          appName: t("app.name"),
        })}
      </footer>
    </div>
  )
}

export default LoginPage
