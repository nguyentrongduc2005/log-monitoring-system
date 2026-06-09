import { createContext, useContext } from "react";
import type { components } from "@/api/generated/api-types";
import type { AuthSession } from "@/features/auth/auth-storage";

type LoginRequest = components["schemas"]["LoginRequest"];

export type AuthContextValue = {
  session: AuthSession | null;
  isInitializing: boolean;
  login: (credentials: LoginRequest) => Promise<AuthSession>;
  logout: () => void;
};

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
