import { cn } from "cn"

import {
  TIRE_ICON_PATHS,
  TIRE_ICON_VIEW_BOX,
} from "@/components/brand/tire-icon-path"

function TireIcon({ className, ...props }: React.ComponentProps<"svg">) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox={TIRE_ICON_VIEW_BOX}
      fill="currentColor"
      aria-hidden="true"
      focusable="false"
      data-slot="tire-icon"
      className={cn("size-4 shrink-0", className)}
      {...props}
    >
      {TIRE_ICON_PATHS.map((d) => (
        <path key={d.slice(0, 32)} fillRule="evenodd" d={d} />
      ))}
    </svg>
  )
}

export { TireIcon }
