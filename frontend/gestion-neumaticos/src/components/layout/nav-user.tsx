import * as React from "react"

import {
  ChevronsUpDownIcon,
  LanguagesIcon,
  Loader2Icon,
  LogOutIcon,
} from "lucide-react"
import { useTranslation } from "react-i18next"

import { LanguageRadioGroup } from "@/components/language-toggle"
import { useActiveThemeOption } from "@/components/theme-options"
import { ThemeRadioGroup } from "@/components/theme-toggle"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar"
import { useCurrentUser } from "@/features/auth/hooks/use-current-user"
import { useLogout } from "@/features/auth/hooks/use-logout"
import { useActiveLanguageShort } from "@/hooks/use-active-language-short"

export function NavUser() {
  const { t } = useTranslation()
  const { isMobile } = useSidebar()
  const user = useCurrentUser()
  const logout = useLogout()
  const activeTheme = useActiveThemeOption()
  const ActiveThemeIcon = activeTheme.icon
  const activeLanguageShort = useActiveLanguageShort()
  const [confirmOpen, setConfirmOpen] = React.useState(false)

  if (!user) {
    return null
  }

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger
            render={
              <SidebarMenuButton
                size="lg"
                aria-label={t("user.menu")}
                className="data-popup-open:bg-sidebar-accent data-popup-open:text-sidebar-accent-foreground"
              />
            }
          >
            <Avatar className="size-8 rounded-lg">
              <AvatarImage src={user.avatarUrl} alt={user.name} />
              <AvatarFallback className="rounded-lg">
                {user.initials}
              </AvatarFallback>
            </Avatar>
            <div className="grid flex-1 text-left text-sm leading-tight">
              <span className="truncate font-medium">{user.name}</span>
              <span className="truncate text-xs text-sidebar-foreground/70">
                {user.email}
              </span>
            </div>
            <ChevronsUpDownIcon className="ml-auto size-4" />
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="min-w-56 rounded-lg"
            side={isMobile ? "bottom" : "right"}
            align="end"
            sideOffset={4}
          >
            <DropdownMenuGroup>
              <DropdownMenuLabel className="p-0 font-normal">
                <div className="flex items-center gap-2 px-1 py-1.5 text-left text-sm">
                  <Avatar className="size-8 rounded-lg">
                    <AvatarImage src={user.avatarUrl} alt={user.name} />
                    <AvatarFallback className="rounded-lg">
                      {user.initials}
                    </AvatarFallback>
                  </Avatar>
                  <div className="grid flex-1 text-left text-sm leading-tight">
                    <span className="truncate font-medium">{user.name}</span>
                    <span className="truncate text-xs text-muted-foreground">
                      {user.email}
                    </span>
                  </div>
                </div>
              </DropdownMenuLabel>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuSub>
                <DropdownMenuSubTrigger>
                  <ActiveThemeIcon />
                  {t("theme.label")}
                  <span className="ml-auto text-xs text-muted-foreground">
                    {t(activeTheme.labelKey)}
                  </span>
                </DropdownMenuSubTrigger>
                <DropdownMenuSubContent>
                  <ThemeRadioGroup />
                </DropdownMenuSubContent>
              </DropdownMenuSub>
              <DropdownMenuSub>
                <DropdownMenuSubTrigger>
                  <LanguagesIcon />
                  {t("language.label")}
                  <span className="ml-auto text-xs text-muted-foreground">
                    {activeLanguageShort}
                  </span>
                </DropdownMenuSubTrigger>
                <DropdownMenuSubContent>
                  <LanguageRadioGroup />
                </DropdownMenuSubContent>
              </DropdownMenuSub>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuGroup>
              <DropdownMenuItem onClick={() => setConfirmOpen(true)}>
                <LogOutIcon />
                {t("user.logout")}
              </DropdownMenuItem>
            </DropdownMenuGroup>
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Confirmación explícita antes de cerrar la sesión (HU-02). */}
        <AlertDialog open={confirmOpen} onOpenChange={setConfirmOpen}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>
                {t("user.logoutConfirm.title")}
              </AlertDialogTitle>
              <AlertDialogDescription>
                {t("user.logoutConfirm.description")}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={logout.isPending}>
                {t("user.logoutConfirm.cancel")}
              </AlertDialogCancel>
              <AlertDialogAction
                variant="destructive"
                disabled={logout.isPending}
                onClick={() => logout.mutate()}
              >
                {logout.isPending ? (
                  <Loader2Icon className="animate-spin" />
                ) : null}
                {t("user.logoutConfirm.confirm")}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
