/**
 * Única fuente de verdad de los paths de la aplicación: los consumen el router
 * (`src/app/router.tsx`) y la navegación (`src/lib/navigation.ts`), para que un
 * renombre de ruta no pueda quedar a medias entre ambos.
 */
export const ROUTES = {
  login: "/login",
  home: "/home",
  transportUnits: "/transport-units",
  tires: "/tires",
  repairs: "/repairs",
  storage: "/storage",
  settings: "/settings",
  audit: "/audit",
} as const

export type AppRoute = (typeof ROUTES)[keyof typeof ROUTES]
