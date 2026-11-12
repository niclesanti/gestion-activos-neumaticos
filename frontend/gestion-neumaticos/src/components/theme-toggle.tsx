import { useTranslation } from "react-i18next"

import { themeOptions, useActiveThemeOption } from "@/components/theme-options"
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

export function ThemeRadioGroup() {
  const { t } = useTranslation()
  const { theme, setTheme } = useTheme()

  return (
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
  )
}

export function ThemeToggle() {
  const { t } = useTranslation()
  const { icon: ActiveIcon } = useActiveThemeOption()

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
          <ThemeRadioGroup />
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
