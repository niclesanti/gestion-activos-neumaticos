import * as React from "react"

const MOBILE_BREAKPOINT = 768
const QUERY = `(max-width: ${MOBILE_BREAKPOINT - 1}px)`

function subscribe(onChange: () => void) {
  const mql = window.matchMedia(QUERY)
  mql.addEventListener("change", onChange)
  return () => mql.removeEventListener("change", onChange)
}

function getSnapshot() {
  return window.matchMedia(QUERY).matches
}

export function useIsMobile() {
  // useSyncExternalStore en vez de useState + useEffect: evita el render
  // intermedio en escritorio y el aviso de react-hooks por setState en efecto.
  return React.useSyncExternalStore(subscribe, getSnapshot, () => false)
}
