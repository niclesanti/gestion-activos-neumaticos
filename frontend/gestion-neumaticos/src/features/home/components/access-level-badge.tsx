import { EyeIcon, PencilIcon, ShieldCheckIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { Badge } from "@/components/ui/badge"
import type { AccessLevel } from "@/lib/access"

const LEVEL_STYLE = {
  ROLE_ADMINISTRADOR: { variant: "default", icon: ShieldCheckIcon },
  ROLE_EDITOR: { variant: "secondary", icon: PencilIcon },
  ROLE_LECTOR: { variant: "outline", icon: EyeIcon },
} as const satisfies Record<
  AccessLevel,
  { variant: "default" | "secondary" | "outline"; icon: typeof EyeIcon }
>

export function AccessLevelBadge({ level }: { level: AccessLevel }) {
  const { t } = useTranslation()
  const { variant, icon: Icon } = LEVEL_STYLE[level]

  return (
    <Badge variant={variant}>
      <Icon data-icon="inline-start" />
      {t(`accessLevel.${level}`)}
    </Badge>
  )
}
