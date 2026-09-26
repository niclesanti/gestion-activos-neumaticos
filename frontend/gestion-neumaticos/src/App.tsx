import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom"

import { AuditPage } from "@/features/audit/pages/AuditPage"
import { LoginPage } from "@/features/auth/pages/LoginPage"
import { HomePage } from "@/features/home/pages/HomePage"
import { SettingsPage } from "@/features/settings/pages/SettingsPage"
import { AppLayout } from "@/layouts/app-layout"

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<AppLayout />}>
          <Route path="/home" element={<HomePage />} />
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
