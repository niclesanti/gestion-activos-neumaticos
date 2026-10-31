import { useTranslation } from "react-i18next"

import { AccessLevelBadge } from "@/features/home/components/access-level-badge"
import { useDayPeriod } from "@/features/home/hooks/use-day-period"
import { useSessionUser } from "@/lib/current-user"

/** Bienvenida: saludo según la hora, nombre de pila, fecha y nivel de acceso. */
export function WelcomeHeader() {
  const { t, i18n } = useTranslation("home")
  const user = useSessionUser()
  const { period, date } = useDayPeriod()

  if (!user) {
    return null
  }

  const firstName = user.name.split(/\s+/)[0]
  const today = new Intl.DateTimeFormat(i18n.resolvedLanguage, {
    weekday: "long",
    day: "numeric",
    month: "long",
  }).format(date)

  return (
    <section
      data-slot="welcome-header"
      className="grid gap-x-4 gap-y-2 sm:grid-cols-[1fr_auto]"
    >
      <h1 className="font-heading text-3xl font-semibold break-words sm:col-start-1 sm:row-start-1">
        <span className="block text-muted-foreground">
          {t(`greeting.${period}`)}
        </span>
        <span className="block">{firstName}</span>
      </h1>
      <time
        dateTime={date.toISOString()}
        className="text-sm text-muted-foreground first-letter:uppercase sm:col-start-2 sm:row-start-1 sm:text-right"
      >
        {today}
      </time>
      <div className="sm:col-start-1 sm:row-start-2">
        <AccessLevelBadge level={user.accessLevel} />
      </div>
    </section>
  )
}
