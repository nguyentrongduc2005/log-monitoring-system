import type { components } from "@/api/generated/api-types";

type LoginResponse = components["schemas"]["LoginResponse"];
type UserResponse = components["schemas"]["UserResponse"];

export type AuthSession = LoginResponse & {
  accessToken: string;
  refreshToken: string;
  user: UserResponse;
};

const AUTH_SESSION_KEY = "logpulse.auth.session";

export function isAuthSession(value: unknown): value is AuthSession {
  if (typeof value !== "object" || value === null) {
    return false;
  }

  const session = value as LoginResponse;

  return (
    typeof session.accessToken === "string" &&
    session.accessToken.length > 0 &&
    typeof session.refreshToken === "string" &&
    session.refreshToken.length > 0 &&
    typeof session.user === "object" &&
    session.user !== null
  );
}

export function isAccessTokenExpired(accessToken: string) {
  try {
    const payloadPart = accessToken.split(".")[1];

    if (!payloadPart) {
      return true;
    }

    const normalized = payloadPart.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(
      normalized.length + ((4 - (normalized.length % 4)) % 4),
      "="
    );
    const payload = JSON.parse(atob(padded)) as { exp?: unknown };

    return (
      typeof payload.exp !== "number" ||
      payload.exp * 1000 <= Date.now() + 30_000
    );
  } catch {
    return true;
  }
}

export function getStoredSession(): AuthSession | null {
  const value = localStorage.getItem(AUTH_SESSION_KEY);

  if (!value) {
    return null;
  }

  try {
    const session: unknown = JSON.parse(value);

    if (isAuthSession(session)) {
      return session;
    }
  } catch {
    // Invalid persisted data is cleared below.
  }

  localStorage.removeItem(AUTH_SESSION_KEY);
  return null;
}

export function storeSession(session: AuthSession) {
  localStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session));
}

export function clearStoredSession() {
  localStorage.removeItem(AUTH_SESSION_KEY);
}
