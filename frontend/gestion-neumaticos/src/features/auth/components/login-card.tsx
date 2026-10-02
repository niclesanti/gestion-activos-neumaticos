import * as React from "react"

import { zodResolver } from "@hookform/resolvers/zod"
import type { ParseKeys } from "i18next"
import { EyeIcon, EyeOffIcon, Loader2Icon } from "lucide-react"
import { useForm } from "react-hook-form"
import { useTranslation } from "react-i18next"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import {
  Field,
  FieldError,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { useLogin } from "@/features/auth/hooks/use-login"
import {
  loginSchema,
  type LoginInput,
  type LoginValues,
} from "@/features/auth/schemas/login-schema"
import { getHttpStatus } from "@/lib/api/client"
import { notify } from "@/lib/toast"

type ErrorKey = "login.error.invalidCredentials" | "login.error.unexpected"

/** Los mensajes de zod son claves del namespace `auth` (ver login-schema). */
type AuthKey = ParseKeys<"auth">

export function LoginCard() {
  const { t } = useTranslation("auth")
  const login = useLogin()
  const [showPassword, setShowPassword] = React.useState(false)
  // Se guarda la clave de traducción, no el texto: así el mensaje se retraduce
  // solo si el usuario cambia de idioma con el error visible.
  const [errorKey, setErrorKey] = React.useState<ErrorKey | null>(null)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginInput, unknown, LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { identifier: "", password: "" },
  })

  const onSubmit = (values: LoginValues) => {
    setErrorKey(null)
    // Al guardarse la sesión, GuestOnly redirige (al tablero o a la pantalla
    // pedida): no se navega desde acá porque el componente ya se desmontó.
    login.mutate(values, {
      onError: (error) => {
        // Mensaje genérico: nunca se distingue usuario inexistente de clave
        // incorrecta. Se avisa por los dos canales: inline, anclado al
        // formulario, y con un toast, que se ve aunque el foco esté en otro lado.
        const key: ErrorKey =
          getHttpStatus(error) === 401
            ? "login.error.invalidCredentials"
            : "login.error.unexpected"
        setErrorKey(key)
        notify.error({ titleKey: `auth:${key}` })
      },
    })
  }

  const identifierError = errors.identifier?.message
  const passwordError = errors.password?.message
  const isSubmitting = login.isPending

  return (
    <Card className="w-full max-w-sm border border-border [--card-spacing:--spacing(4)]">
      <CardHeader>
        <CardTitle>{t("login.title")}</CardTitle>
        <CardDescription>{t("login.description")}</CardDescription>
      </CardHeader>
      <CardContent>
        {/* noValidate: la validación la hace zod, con mensajes traducidos, en
            lugar de los globos nativos del navegador. */}
        <form id="login-form" noValidate onSubmit={handleSubmit(onSubmit)}>
          <FieldGroup>
            {errorKey ? (
              <p
                role="alert"
                className="rounded-3xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
              >
                {t(errorKey)}
              </p>
            ) : null}
            <Field data-invalid={identifierError ? true : undefined}>
              <FieldLabel htmlFor="identifier">
                {t("login.identifier.label")}
              </FieldLabel>
              {/* Acepta correo electrónico o nombre de usuario, por eso type="text". */}
              <Input
                id="identifier"
                type="text"
                placeholder={t("login.identifier.placeholder")}
                autoComplete="username"
                autoCapitalize="none"
                spellCheck={false}
                aria-required
                aria-invalid={Boolean(identifierError) || errorKey !== null}
                aria-describedby={
                  identifierError ? "identifier-error" : undefined
                }
                {...register("identifier")}
              />
              {identifierError ? (
                <FieldError id="identifier-error">
                  {t(identifierError as AuthKey)}
                </FieldError>
              ) : null}
            </Field>
            <Field data-invalid={passwordError ? true : undefined}>
              <div className="flex items-center">
                <FieldLabel htmlFor="password">
                  {t("login.password.label")}
                </FieldLabel>
              </div>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder={t("login.password.placeholder")}
                  autoComplete="current-password"
                  aria-required
                  aria-invalid={Boolean(passwordError) || errorKey !== null}
                  aria-describedby={
                    passwordError ? "password-error" : undefined
                  }
                  className="pr-11"
                  {...register("password")}
                />
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-sm"
                  aria-label={
                    showPassword
                      ? t("login.password.hide")
                      : t("login.password.show")
                  }
                  aria-pressed={showPassword}
                  aria-controls="password"
                  onClick={() => setShowPassword((visible) => !visible)}
                  className="absolute top-1/2 right-1 -translate-y-1/2 text-muted-foreground"
                >
                  {showPassword ? <EyeOffIcon /> : <EyeIcon />}
                </Button>
              </div>
              {passwordError ? (
                <FieldError id="password-error">
                  {t(passwordError as AuthKey)}
                </FieldError>
              ) : null}
            </Field>
          </FieldGroup>
        </form>
      </CardContent>
      <CardFooter className="flex-col gap-2 border-t">
        <Button
          type="submit"
          form="login-form"
          className="w-full"
          disabled={isSubmitting}
        >
          {isSubmitting ? <Loader2Icon className="animate-spin" /> : null}
          {isSubmitting ? t("login.submitting") : t("login.submit")}
        </Button>
      </CardFooter>
    </Card>
  )
}
