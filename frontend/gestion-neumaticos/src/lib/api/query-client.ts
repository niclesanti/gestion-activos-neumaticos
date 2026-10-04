import { QueryClient } from "@tanstack/react-query"

import { getHttpStatus } from "@/lib/api/client"

/** Errores 4xx no se reintentan: repetir la misma request no los arregla. */
function shouldRetry(failureCount: number, error: unknown) {
  const status = getHttpStatus(error)
  if (status !== undefined && status >= 400 && status < 500) {
    return false
  }
  return failureCount < 2
}

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: shouldRetry,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
    },
  },
})
