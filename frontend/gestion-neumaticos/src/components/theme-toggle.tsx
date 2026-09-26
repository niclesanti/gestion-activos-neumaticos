import { MonitorIcon, MoonIcon, SunIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { useTheme, type Theme } from "@/components/theme-provider"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

const themeOptions = [
  { value: "light", labelKey: "theme.light", icon: SunIcon },
  { value: "dark", labelKey: "theme.dark", icon: MoonIcon },
  { value: "system", labelKey: "theme.system", icon: MonitorIcon },
] satisfies {
  value: Theme
  labelKey: "theme.light" | "theme.dark" | "theme.system"
  icon: typeof SunIcon
}[]

export function ThemeToggle() {
  const { t } = useTranslation()
  const { theme, setTheme } = useTheme()
  const activeOption = themeOptions.find((option) => option.value === theme)
  const ActiveIcon = activeOption?.icon ?? MonitorIcon

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button variant="ghost" size="icon" aria-label={t("theme.toggle")} />
        }
      >
        <ActiveIcon />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuGroup>
          <DropdownMenuRadioGroup
            value={theme}
            onValueChange={(value) => setTheme(value as Theme)}
          >
            {themeOptions.map(({ value, labelKey, icon: Icon }) => (
              <DropdownMenuRadioItem key={value} value={value}>
                <Icon />
                {t(labelKey)}
              </DropdownMenuRadioItem>
            ))}
          </DropdownMenuRadioGroup>
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
