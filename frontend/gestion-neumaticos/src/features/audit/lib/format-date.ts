const MINUTE_MS = 60_000
const HOUR_MS = 60 * MINUTE_MS
const DAY_MS = 24 * HOUR_MS

/** Fecha y hora completas en el idioma de la interfaz ("31 oct 2026, 17:27"). */
export function formatAbsolute(iso: string, locale: string | undefined) {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(iso))
}

/** Fecha relativa ("hace 3 días", "ayer") en el idioma de la interfaz. */
export function formatRelative(
  iso: string,
  locale: string | undefined,
  nowMs: number = Date.now()
) {
  const rtf = new Intl.RelativeTimeFormat(locale, { numeric: "auto" })
  const diff = new Date(iso).getTime() - nowMs
  const abs = Math.abs(diff)

  if (abs < MINUTE_MS) {
    return rtf.format(0, "second")
  }
  if (abs < HOUR_MS) {
    return rtf.format(Math.round(diff / MINUTE_MS), "minute")
  }
  if (abs < DAY_MS) {
    return rtf.format(Math.round(diff / HOUR_MS), "hour")
  }
  if (abs < 30 * DAY_MS) {
    return rtf.format(Math.round(diff / DAY_MS), "day")
  }
  if (abs < 365 * DAY_MS) {
    return rtf.format(Math.round(diff / (30 * DAY_MS)), "month")
  }
  return rtf.format(Math.round(diff / (365 * DAY_MS)), "year")
}
