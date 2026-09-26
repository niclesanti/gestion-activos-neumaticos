import { Outlet } from "react-router-dom"

import { AppHeader } from "@/components/layout/app-header"
import { AppSidebar } from "@/components/layout/app-sidebar"
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar"

/** Nombre de la cookie que escribe SidebarProvider al abrir/cerrar la sidebar. */
const SIDEBAR_COOKIE_NAME = "sidebar_state"

/**
 * SidebarProvider persiste el estado en una cookie pero no la lee (en el
 * ejemplo de shadcn eso lo hace el servidor). Al ser una SPA la leemos acá.
 */
function readSidebarState() {
  const match = document.cookie.match(
    new RegExp(`(?:^|; )${SIDEBAR_COOKIE_NAME}=([^;]*)`)
  )
  return match ? match[1] !== "false" : true
}

/**
 * Shell de la aplicación: sidebar y header fijos, y scroll confinado al
 * contenido de la página (h-svh + overflow-hidden en el marco, overflow-y-auto
 * sólo en el contenedor del Outlet).
 */
function AppLayout() {
  return (
    <SidebarProvider
      defaultOpen={readSidebarState()}
      className="h-svh overflow-hidden"
    >
      <AppSidebar />
      <SidebarInset className="h-svh overflow-hidden">
        <AppHeader />
        <div className="flex-1 overflow-y-auto p-4">
          <Outlet />
        </div>
      </SidebarInset>
    </SidebarProvider>
  )
}

export { AppLayout }
export default AppLayout
