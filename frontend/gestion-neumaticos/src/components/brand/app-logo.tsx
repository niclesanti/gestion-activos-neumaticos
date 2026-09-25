import { cn } from "cn"

import { TireIcon } from "@/components/brand/tire-icon"
import { APP_NAME } from "@/lib/constants"

function AppLogo({
  className,
  showText = true,
  ...props
}: React.ComponentProps<"div"> & { showText?: boolean }) {
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
          {APP_NAME}
        </span>
      ) : (
        <span className="sr-only">{APP_NAME}</span>
      )}
    </div>
  )
}

export { AppLogo }
