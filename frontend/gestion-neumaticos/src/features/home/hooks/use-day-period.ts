import * as React from "react"

export type DayPeriod = "morning" | "afternoon" | "evening"

const REFRESH_MS = 60_000

function getDayPeriod(date: Date): DayPeriod {
  const hour = date.getHours()
  if (hour >= 5 && hour < 12) {
    return "morning"
  }
  if (hour >= 12 && hour < 20) {
    return "afternoon"
  }
  return "evening"
}

/**
 * Momento del día según el reloj del dispositivo y la fecha de hoy. Se
 * refresca cada minuto para que el saludo cambie con la pestaña abierta.
 */
export function useDayPeriod() {
  const [now, setNow] = React.useState(() => new Date())

  React.useEffect(() => {
    const id = window.setInterval(() => setNow(new Date()), REFRESH_MS)
    return () => window.clearInterval(id)
  }, [])

  return { period: getDayPeriod(now), date: now }
}
