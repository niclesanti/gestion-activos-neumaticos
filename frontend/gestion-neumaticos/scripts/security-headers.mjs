/**
 * Cabeceras de seguridad del frontend: única definición, compartida por el
 * plugin de Vite que escribe `dist/_headers` (Cloudflare) y por el script que
 * genera el snippet de nginx (imagen Docker).
 *
 * La CSP es estricta en scripts ('self', sin inline ni eval): un XSS no puede
 * cargar código propio ni exfiltrar el token de `sessionStorage` a otro origen,
 * porque `connect-src` solo permite la propia app y la API.
 * `style-src` admite 'unsafe-inline' porque los componentes (Base UI, el
 * ThemeProvider) inyectan estilos en tiempo de ejecución; los estilos no
 * ejecutan código.
 */

/** Origen (esquema + host + puerto) de la URL de la API. */
export function apiOrigin(apiUrl) {
  return new URL(apiUrl).origin
}

/** @returns {Array<[string, string]>} pares nombre/valor, en orden. */
export function securityHeaders(apiUrl) {
  const csp = [
    "default-src 'self'",
    "script-src 'self'",
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data:",
    "font-src 'self'",
    `connect-src 'self' ${apiOrigin(apiUrl)}`,
    "manifest-src 'self'",
    "worker-src 'self'",
    "object-src 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "frame-ancestors 'none'",
  ].join("; ")

  return [
    ["Content-Security-Policy", csp],
    ["X-Content-Type-Options", "nosniff"],
    ["X-Frame-Options", "DENY"],
    ["Referrer-Policy", "strict-origin-when-cross-origin"],
    [
      "Permissions-Policy",
      "camera=(), microphone=(), geolocation=(), payment=(), usb=()",
    ],
    ["Cross-Origin-Opener-Policy", "same-origin"],
    ["Cross-Origin-Resource-Policy", "same-origin"],
    // Los navegadores la ignoran sobre HTTP: solo rige detrás de TLS.
    ["Strict-Transport-Security", "max-age=31536000; includeSubDomains"],
  ]
}

/** Formato `_headers` de Cloudflare (Workers static assets / Pages). */
export function cloudflareHeaders(apiUrl) {
  const lines = securityHeaders(apiUrl).map(
    ([name, value]) => `  ${name}: ${value}`
  )
  return `/*\n${lines.join("\n")}\n`
}

/** Directivas `add_header` de nginx (`always`: también en respuestas de error). */
export function nginxHeaders(apiUrl) {
  const lines = securityHeaders(apiUrl).map(
    ([name, value]) => `add_header ${name} "${value}" always;`
  )
  return `# Generado por scripts/nginx-security-headers.mjs: no editar.\n${lines.join("\n")}\n`
}
