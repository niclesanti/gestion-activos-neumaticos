import { AppLogo } from "@/components/brand/app-logo"
import { ThemeToggle } from "@/components/theme-toggle"
import { LoginCard } from "@/features/auth/components/LoginCard"
import { APP_NAME } from "@/lib/constants"

export function LoginPage() {
  return (
    <div className="flex min-h-svh flex-col bg-background">
      <header className="flex items-center justify-between px-6 py-4">
        <AppLogo />
        <ThemeToggle />
      </header>
      <main className="flex flex-1 items-center justify-center px-6 py-8">
        <LoginCard />
      </main>
      <footer className="px-6 py-6 text-center text-xs text-muted-foreground">
        © {new Date().getFullYear()} · {APP_NAME}
      </footer>
    </div>
  )
}

export default LoginPage
