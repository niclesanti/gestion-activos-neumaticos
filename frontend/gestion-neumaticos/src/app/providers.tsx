import { LanguageProvider } from "@/components/language-provider"
import { ThemeProvider } from "@/components/theme-provider"

/**
 * Único punto de composición de los providers globales. Todo provider nuevo
 * (QueryClientProvider, Toaster de sonner, etc.) se agrega acá y no en main.tsx.
 */
export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider>
      <LanguageProvider>{children}</LanguageProvider>
    </ThemeProvider>
  )
}
