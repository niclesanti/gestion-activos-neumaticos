import { toast, type ToastData, type ToastKey } from "@/components/ui/toast"

/**
 * API única para disparar notificaciones desde cualquier parte de la app, para
 * que un feature no tenga que conocer el manager de Base UI.
 *
 * Se pasan claves de i18next (`titleKey`) y no texto ya traducido: el texto se
 * resuelve en `ToastList` en cada render, así un toast visible se retraduce al
 * cambiar de idioma. `title` / `description` quedan como escape hatch para
 * datos que no son traducibles (por ejemplo un código de neumático del backend).
 *
 * Las claves de un namespace distinto de `common` van prefijadas: `"auth:login.error.invalidCredentials"`.
 */
export type NotifyOptions = {
  titleKey?: ToastKey
  descriptionKey?: ToastKey
  /** Interpolación aplicada a todas las claves del toast. */
  values?: Record<string, unknown>
  /** Texto crudo, sin traducir. Ignorado si se pasó la clave equivalente. */
  title?: string
  description?: string
  /** Etiqueta del botón de acción. Requiere `onAction`. */
  actionLabelKey?: ToastKey
  onAction?: () => void
  /** Milisegundos hasta el cierre automático. `0` deja el toast abierto. */
  timeout?: number
  /** Reutilizar un id existente actualiza ese toast en lugar de apilar uno nuevo. */
  id?: string
  onClose?: () => void
}

/** Tipos soportados por `ToastIcon`. El default no lleva ícono. */
type ToastType = "success" | "info" | "warning" | "error" | "loading"

function toPayload(options: NotifyOptions, type?: ToastType) {
  const { titleKey, descriptionKey, actionLabelKey, values } = options
  const data: ToastData = {
    titleKey,
    descriptionKey,
    actionLabelKey,
    values,
  }

  return {
    ...(options.id === undefined ? {} : { id: options.id }),
    type,
    title: options.title,
    description: options.description,
    timeout: options.timeout,
    onClose: options.onClose,
    ...(options.onAction === undefined
      ? {}
      : { actionProps: { onClick: options.onAction } }),
    data,
  }
}

function add(options: NotifyOptions, type?: ToastType) {
  return toast.add(toPayload(options, type))
}

/** Toast neutro, sin ícono. */
function show(options: NotifyOptions) {
  return add(options)
}

function success(options: NotifyOptions) {
  return add(options, "success")
}

function info(options: NotifyOptions) {
  return add(options, "info")
}

function warning(options: NotifyOptions) {
  return add(options, "warning")
}

function error(options: NotifyOptions) {
  return add(options, "error")
}

/** Spinner sin cierre automático: se resuelve con `notify.update` o `notify.close`. */
function loading(options: NotifyOptions) {
  return add({ timeout: 0, ...options }, "loading")
}

function update(id: string, options: NotifyOptions, type?: ToastType) {
  toast.update(id, toPayload(options, type))
}

function close(id: string) {
  toast.close(id)
}

type PromiseMessages<T> = {
  loading: NotifyOptions
  success: NotifyOptions | ((value: T) => NotifyOptions)
  error: NotifyOptions | ((reason: unknown) => NotifyOptions)
}

function resolveMessage<TArg>(
  message: NotifyOptions | ((arg: TArg) => NotifyOptions),
  arg: TArg
) {
  return typeof message === "function" ? message(arg) : message
}

/**
 * Acompaña una promesa con un único toast que pasa de `loading` a `success` o
 * `error`. No usa `toast.promise()` de Base UI a propósito: esa API recibe
 * strings ya resueltos y congelaría el idioma del toast en vuelo.
 */
function promise<T>(input: Promise<T>, messages: PromiseMessages<T>) {
  const id = loading(messages.loading)

  input.then(
    (value) => update(id, resolveMessage(messages.success, value), "success"),
    (reason: unknown) =>
      update(id, resolveMessage(messages.error, reason), "error")
  )

  return input
}

export const notify = {
  show,
  success,
  info,
  warning,
  error,
  loading,
  promise,
  update,
  close,
}
