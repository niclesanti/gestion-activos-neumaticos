import path from "path"
import tailwindcss from "@tailwindcss/vite"
import react from "@vitejs/plugin-react"
import { defineConfig, loadEnv, type Plugin } from "vite"

import { cloudflareHeaders } from "./scripts/security-headers.mjs"

/**
 * Escribe `dist/_headers` con las cabeceras de seguridad (CSP incluida) para
 * Cloudflare. La CSP solo permite conectarse a la API de VITE_API_URL, así que
 * el build falla si no está definida.
 */
function securityHeadersPlugin(apiUrl: string | undefined): Plugin {
  return {
    name: "security-headers",
    apply: "build",
    generateBundle() {
      if (!apiUrl || !URL.canParse(apiUrl)) {
        this.error(
          "VITE_API_URL debe ser la URL de la API (ej. VITE_API_URL=https://api.ejemplo.com npm run build)"
        )
      }
      this.emitFile({
        type: "asset",
        fileName: "_headers",
        source: cloudflareHeaders(apiUrl),
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, import.meta.dirname, "VITE_")

  return {
    plugins: [react(), tailwindcss(), securityHeadersPlugin(env.VITE_API_URL)],
    resolve: {
      alias: {
        "@": path.resolve(import.meta.dirname, "./src"),
      },
    },
  }
})
