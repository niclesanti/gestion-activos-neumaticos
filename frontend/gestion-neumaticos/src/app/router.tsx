import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom"

import { AppLayout } from "@/components/layout/app-layout"
import { AuditPage } from "@/features/audit/pages/audit-page"
import { GuestOnly } from "@/features/auth/components/guest-only"
import { RequireAuth } from "@/features/auth/components/require-auth"
import { RequireRouteAccess } from "@/features/auth/components/require-route-access"
import { LoginPage } from "@/features/auth/pages/login-page"
import { ToastShowcasePage } from "@/features/dev/pages/toast-showcase-page"
import { HomePage } from "@/features/home/pages/home-page"
import { RepairsPage } from "@/features/repairs/pages/repairs-page"
import { SettingsPage } from "@/features/settings/pages/settings-page"
import { StoragePage } from "@/features/storage/pages/storage-page"
import { TiresPage } from "@/features/tires/pages/tires-page"
import { TransportUnitsPage } from "@/features/transport-units/pages/transport-units-page"
import { ROUTES } from "@/lib/routes"

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Con sesión iniciada, el login redirige al inicio. */}
        <Route element={<GuestOnly />}>
          <Route path={ROUTES.login} element={<LoginPage />} />
        </Route>
        {/* Sin sesión, todo lo de adentro redirige al login. */}
        <Route element={<RequireAuth />}>
          <Route element={<AppLayout />}>
            {/* Bloquea por URL las secciones vedadas al nivel de acceso. */}
            <Route element={<RequireRouteAccess />}>
              <Route path={ROUTES.home} element={<HomePage />} />
              <Route
                path={ROUTES.transportUnits}
                element={<TransportUnitsPage />}
              />
              <Route path={ROUTES.tires} element={<TiresPage />} />
              <Route path={ROUTES.repairs} element={<RepairsPage />} />
              <Route path={ROUTES.storage} element={<StoragePage />} />
              <Route path={ROUTES.settings} element={<SettingsPage />} />
              <Route path={ROUTES.audit} element={<AuditPage />} />
              {import.meta.env.DEV ? (
                <Route path={ROUTES.toasts} element={<ToastShowcasePage />} />
              ) : null}
            </Route>
          </Route>
        </Route>
        {/* `/home` está protegida: sin sesión, RequireAuth lleva al login. */}
        <Route path="/" element={<Navigate to={ROUTES.home} replace />} />
        {/* TODO: reemplazar por una NotFoundPage: hoy la redirección oculta
            links roto y typos en la URL. */}
        <Route path="*" element={<Navigate to={ROUTES.home} replace />} />
      </Routes>
    </BrowserRouter>
  )
}
