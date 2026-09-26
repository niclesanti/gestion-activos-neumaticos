import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom"

import { AuditPage } from "@/features/audit/pages/AuditPage"
import { LoginPage } from "@/features/auth/pages/LoginPage"
import { HomePage } from "@/features/home/pages/HomePage"
import { RepairsPage } from "@/features/repairs/pages/RepairsPage"
import { SettingsPage } from "@/features/settings/pages/SettingsPage"
import { StoragePage } from "@/features/storage/pages/StoragePage"
import { TiresPage } from "@/features/tires/pages/TiresPage"
import { TransportUnitsPage } from "@/features/transport-units/pages/TransportUnitsPage"
import { AppLayout } from "@/layouts/app-layout"

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<AppLayout />}>
          <Route path="/home" element={<HomePage />} />
          <Route path="/transport-units" element={<TransportUnitsPage />} />
          <Route path="/tires" element={<TiresPage />} />
          <Route path="/repairs" element={<RepairsPage />} />
          <Route path="/storage" element={<StoragePage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/audit" element={<AuditPage />} />
        </Route>
        {/* TODO: volver a redirigir a /login cuando existan rutas protegidas. */}
        <Route path="/" element={<Navigate to="/home" replace />} />
        <Route path="*" element={<Navigate to="/home" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
