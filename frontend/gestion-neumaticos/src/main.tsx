import { StrictMode } from "react"
import { createRoot } from "react-dom/client"

import "./index.css"
import "@/lib/i18n"
import App from "./App.tsx"
import { LanguageProvider } from "@/components/language-provider.tsx"
import { ThemeProvider } from "@/components/theme-provider.tsx"

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ThemeProvider>
      <LanguageProvider>
        <App />
      </LanguageProvider>
    </ThemeProvider>
  </StrictMode>
)
