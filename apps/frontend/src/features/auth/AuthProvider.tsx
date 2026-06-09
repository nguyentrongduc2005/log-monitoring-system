import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type PropsWithChildren
} from "react";
import { login as loginRequest, refreshSession } from "@/api/auth";
import { installAuthInterceptors } from "@/api/client";
import { AuthContext } from "@/features/auth/auth-context";
import {
  clearStoredSession,
  getStoredSession,
  isAccessTokenExpired,
  storeSession
} from "@/features/auth/auth-storage";
import type { components } from "@/api/generated/api-types";
import type { AuthSession } from "@/features/auth/auth-storage";

type LoginRequest = components["schemas"]["LoginRequest"];

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);

  const logout = useCallback(() => {
    clearStoredSession();
    setSession(null);
  }, []);

  const login = useCallback(async (credentials: LoginRequest) => {
    const nextSession = await loginRequest(credentials);
    storeSession(nextSession);
    setSession(nextSession);
    return nextSession;
  }, []);

  useEffect(() => {
    let active = true;

    async function restoreSession() {
      const storedSession = getStoredSession();

      if (!storedSession) {
        setIsInitializing(false);
        return;
      }

      try {
        const restoredSession = isAccessTokenExpired(storedSession.accessToken)
          ? await refreshSession(storedSession.refreshToken)
          : storedSession;

        if (active) {
          storeSession(restoredSession);
          setSession(restoredSession);
        }
      } catch {
        clearStoredSession();
      } finally {
        if (active) {
          setIsInitializing(false);
        }
      }
    }

    void restoreSession();

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    return installAuthInterceptors({
      getAccessToken: () => getStoredSession()?.accessToken ?? null,
      refresh: async () => {
        const storedSession = getStoredSession();

        if (!storedSession) {
          throw new Error("No refresh token is available.");
        }

        const refreshedSession = await refreshSession(
          storedSession.refreshToken
        );
        storeSession(refreshedSession);
        setSession(refreshedSession);
        return refreshedSession.accessToken;
      },
      onUnauthorized: logout
    });
  }, [logout]);

  const value = useMemo(
    () => ({ session, isInitializing, login, logout }),
    [session, isInitializing, login, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
