import { useMutation } from "@tanstack/react-query"

import { login } from "@/features/auth/api/auth-api"
import { useSessionStore } from "@/features/auth/store/session-store"

export function useLogin() {
  const setSession = useSessionStore((state) => state.setSession)

  return useMutation({
    mutationFn: login,
    onSuccess: setSession,
  })
}
