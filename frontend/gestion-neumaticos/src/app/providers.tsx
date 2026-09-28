import { LanguageProvider } from "@/components/language-provider"
import { ThemeProvider } from "@/components/theme-provider"
import { Toaster } from "@/components/ui/toast"

/**
 * Único punto de composición de los providers globales. Todo provider nuevo
 * (QueryClientProvider, etc.) se agrega acá y no en main.tsx.
 *
 * El Toaster va dentro de LanguageProvider y por fuera del router, así las
 * notificaciones son globales y también aparecen en /login, que queda fuera de
 * AppLayout.
 */
export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider>
      <LanguageProvider>
        {children}
        <Toaster />
      </LanguageProvider>
    </ThemeProvider>
  )
}
