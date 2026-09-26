export type CurrentUser = {
  name: string
  email: string
  avatarUrl: string
  initials: string
}

/** Primeras letras de las dos primeras palabras del nombre, para el AvatarFallback. */
function getInitials(name: string) {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0]?.toUpperCase() ?? "")
    .join("")
}

// TODO: reemplazar por el usuario autenticado real cuando existan el módulo
// `usuarios` del backend y el endpoint de sesión. Es el único punto a cambiar.
const MOCK_USER = {
  name: "Santiago Nicle",
  email: "santiago.nicle@ejemplo.com",
  avatarUrl: "",
}

export function useCurrentUser(): CurrentUser {
  return {
    ...MOCK_USER,
    initials: getInitials(MOCK_USER.name),
  }
}
