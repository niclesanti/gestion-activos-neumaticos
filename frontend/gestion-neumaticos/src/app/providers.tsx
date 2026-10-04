import { QueryClientProvider } from "@tanstack/react-query"
import { ReactQueryDevtools } from "@tanstack/react-query-devtools"

import { LanguageProvider } from "@/components/language-provider"
import { ThemeProvider } from "@/components/theme-provider"
import { Toaster } from "@/components/ui/toast"
import {
  setupAuthClient,
  syncSessionOnPageRestore,
} from "@/features/auth/api/setup-auth-client"
import { queryClient } from "@/lib/api/query-client"

// Una sola vez, al cargar la app: el cliente HTTP manda el token de la sesión
// y cierra la sesión local si el backend lo rechaza.
setupAuthClient()
// Y que volver con "atrás" a una página en caché no reviva una sesión cerrada.
syncSessionOnPageRestore()

/**
 * Único punto de composición de los providers globales. Todo provider nuevo
 * se agrega acá y no en main.tsx.
 *
 * El Toaster va dentro de LanguageProvider y por fuera del router, así las
 * notificaciones son globales y también aparecen en /login, que queda fuera de
 * AppLayout.
 */
export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <LanguageProvider>
          {children}
          <Toaster />
        </LanguageProvider>
      </ThemeProvider>
      {import.meta.env.DEV ? (
        <ReactQueryDevtools buttonPosition="bottom-left" />
      ) : null}
    </QueryClientProvider>
  )
}
