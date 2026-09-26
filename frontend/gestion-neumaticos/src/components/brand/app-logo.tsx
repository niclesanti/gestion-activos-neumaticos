import { cn } from "cn"
import { useTranslation } from "react-i18next"

import { TireIcon } from "@/components/brand/tire-icon"

function AppLogo({
  className,
  showText = true,
  ...props
}: React.ComponentProps<"div"> & { showText?: boolean }) {
  const { t } = useTranslation()
  const appName = t("app.name")

  return (
    <div
      data-slot="app-logo"
      className={cn("flex items-center gap-2 text-foreground", className)}
      {...props}
    >
      <span className="flex size-9 items-center justify-center">
        <TireIcon className="size-7" />
      </span>
      {showText ? (
        <span className="font-heading text-base font-semibold max-sm:sr-only">
          {appName}
        </span>
      ) : (
        <span className="sr-only">{appName}</span>
      )}
    </div>
  )
}

export { AppLogo }
