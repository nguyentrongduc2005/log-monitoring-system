import { apiClient } from "@/api/client";
import type { components } from "@/api/generated/api-types";
import {
  isAuthSession,
  type AuthSession
} from "@/features/auth/auth-storage";

type LoginRequest = components["schemas"]["LoginRequest"];
type LoginResponse = components["schemas"]["LoginResponse"];
type RefreshTokenRequest = components["schemas"]["RefreshTokenRequest"];

type ApiEnvelope<T> = {
  data?: T;
  message?: string;
  success?: boolean;
};

function getAuthSession(response: ApiEnvelope<LoginResponse>): AuthSession {
  if (!isAuthSession(response.data)) {
    throw new Error("The authentication response is incomplete.");
  }

  return response.data;
}

export async function login(credentials: LoginRequest): Promise<AuthSession> {
  const response = await apiClient.post<{ data: LoginResponse }>(
    "/auth/login",
    credentials,
    { skipAuthRefresh: true }
  );

  return getAuthSession(response.data);
}

export async function refreshSession(
  refreshToken: RefreshTokenRequest["refreshToken"]
): Promise<AuthSession> {
  const response = await apiClient.post<ApiEnvelope<LoginResponse>>(
    "/auth/refresh",
    { refreshToken },
    { skipAuthRefresh: true }
  );

  return getAuthSession(response.data);
}
