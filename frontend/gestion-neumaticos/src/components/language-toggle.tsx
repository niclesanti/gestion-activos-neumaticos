import { LanguagesIcon } from "lucide-react"
import { useTranslation } from "react-i18next"

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { SUPPORTED_LANGUAGES, type Language } from "@/lib/i18n/config"

export function LanguageRadioGroup() {
  const { i18n } = useTranslation()

  return (
    <DropdownMenuRadioGroup
      value={i18n.resolvedLanguage}
      onValueChange={(value) => {
        void i18n.changeLanguage(value as Language)
      }}
    >
      {SUPPORTED_LANGUAGES.map(({ value, label, short }) => (
        <DropdownMenuRadioItem key={value} value={value}>
          <LanguagesIcon />
          {label}
          <span className="ml-auto text-xs text-muted-foreground">{short}</span>
        </DropdownMenuRadioItem>
      ))}
    </DropdownMenuRadioGroup>
  )
}

export function LanguageToggle() {
  const { t } = useTranslation()

  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        render={
          <Button
            variant="ghost"
            size="icon"
            aria-label={t("language.toggle")}
          />
        }
      >
        <LanguagesIcon />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuGroup>
          <LanguageRadioGroup />
        </DropdownMenuGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
