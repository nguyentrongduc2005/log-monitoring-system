import { apiClient } from "@/api/client";
import type { components } from "@/api/generated/api-types";

export type UserResponse = components["schemas"]["UserResponse"];
export type UpdateUserRequest = components["schemas"]["UpdateUserRequest"];
export type ChangePasswordRequest =
  components["schemas"]["ChangePasswordRequest"];

type ApiEnvelope<T> = {
  data?: T;
  message?: string;
  success?: boolean;
};

function requireData<T>(response: ApiEnvelope<T>, fallbackMessage: string): T {
  if (response.data === undefined || response.data === null) {
    throw new Error(response.message || fallbackMessage);
  }

  return response.data;
}

export async function getProfile(): Promise<UserResponse> {
  const response = await apiClient.get<ApiEnvelope<UserResponse>>("/users/me");
  return requireData(response.data, "The profile response is incomplete.");
}

export async function updateProfile(
  request: UpdateUserRequest
): Promise<UserResponse> {
  const response = await apiClient.put<ApiEnvelope<UserResponse>>(
    "/users/me",
    request
  );

  return requireData(response.data, "The profile update response is incomplete.");
}

export async function changePassword(
  request: ChangePasswordRequest
): Promise<void> {
  await apiClient.put<ApiEnvelope<null>>("/users/me/password", request);
}
