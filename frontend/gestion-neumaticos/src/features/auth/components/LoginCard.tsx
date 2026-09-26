import * as React from "react"
import { EyeIcon, EyeOffIcon, Loader2Icon } from "lucide-react"
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
import { Field, FieldGroup, FieldLabel } from "@/components/ui/field"
import { Input } from "@/components/ui/input"

type ErrorKey = "login.error.invalidCredentials"

export function LoginCard() {
  const { t } = useTranslation("auth")
  const [showPassword, setShowPassword] = React.useState(false)
  const [isSubmitting, setIsSubmitting] = React.useState(false)
  // Se guarda la clave de traducción, no el texto: así el mensaje se retraduce
  // solo si el usuario cambia de idioma con el error visible.
  const [errorKey, setErrorKey] = React.useState<ErrorKey | null>(null)

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setErrorKey(null)
    setIsSubmitting(true)

    // TODO: conectar con el endpoint de autenticación del backend.
    // Ante credenciales inválidas: setErrorKey("login.error.invalidCredentials").
    setIsSubmitting(false)
  }

  return (
    <Card className="w-full max-w-sm border border-border [--card-spacing:--spacing(4)]">
      <CardHeader>
        <CardTitle>{t("login.title")}</CardTitle>
        <CardDescription>{t("login.description")}</CardDescription>
      </CardHeader>
      <CardContent>
        <form id="login-form" onSubmit={handleSubmit}>
          <FieldGroup>
            {errorKey ? (
              <p
                role="alert"
                className="rounded-3xl border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
              >
                {t(errorKey)}
              </p>
            ) : null}
            <Field>
              <FieldLabel htmlFor="identifier">
                {t("login.identifier.label")}
              </FieldLabel>
              {/* Acepta correo electrónico o nombre de usuario, por eso type="text". */}
              <Input
                id="identifier"
                name="identifier"
                type="text"
                placeholder={t("login.identifier.placeholder")}
                autoComplete="username"
                autoCapitalize="none"
                spellCheck={false}
                aria-invalid={errorKey !== null}
                required
              />
            </Field>
            <Field>
              <div className="flex items-center">
                <FieldLabel htmlFor="password">
                  {t("login.password.label")}
                </FieldLabel>
              </div>
              <div className="relative">
                <Input
                  id="password"
                  name="password"
                  type={showPassword ? "text" : "password"}
                  placeholder={t("login.password.placeholder")}
                  autoComplete="current-password"
                  aria-invalid={errorKey !== null}
                  className="pr-11"
                  required
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
