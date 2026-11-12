import { MonitorIcon, MoonIcon, SunIcon } from "lucide-react"

import { useTheme, type Theme } from "@/components/theme-provider"

export const themeOptions = [
  { value: "light", labelKey: "theme.light", icon: SunIcon },
  { value: "dark", labelKey: "theme.dark", icon: MoonIcon },
  { value: "system", labelKey: "theme.system", icon: MonitorIcon },
] satisfies {
  value: Theme
  labelKey: "theme.light" | "theme.dark" | "theme.system"
  icon: typeof SunIcon
}[]

export function useActiveThemeOption() {
  const { theme } = useTheme()
  return (
    themeOptions.find((option) => option.value === theme) ??
    themeOptions[themeOptions.length - 1]
  )
}
