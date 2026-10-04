// Escribe en stdout el snippet de nginx con las cabeceras de seguridad, para
// la URL de la API de VITE_API_URL. Lo usa el Dockerfile.
import { nginxHeaders } from "./security-headers.mjs"

const apiUrl = process.env.VITE_API_URL
if (!apiUrl) {
  console.error("Falta VITE_API_URL")
  process.exit(1)
}
process.stdout.write(nginxHeaders(apiUrl))
