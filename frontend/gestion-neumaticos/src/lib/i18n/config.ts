export const LANGUAGE_STORAGE_KEY = "language"

export const SUPPORTED_LANGUAGES = [
  { value: "es", label: "Español", short: "ESP" },
  { value: "en", label: "English", short: "ENG" },
] as const

export type Language = (typeof SUPPORTED_LANGUAGES)[number]["value"]

export const FALLBACK_LANGUAGE: Language = "es"

export const NAMESPACES = ["common", "auth"] as const

export const DEFAULT_NAMESPACE = "common"
