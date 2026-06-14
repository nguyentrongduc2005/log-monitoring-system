import { apiClient } from "@/api/client";
import { isAxiosError } from "axios";
import type {
  ApiEnvelope,
  Application,
  ApplicationAccess,
  ApplicationGrant,
  CreateUserRequest,
  UpdateUserRequest,
  User,
  UserListQuery,
  UserPage,
  UserRole,
  UserStatus
} from "./user-access-types";

function requireData<T>(
  envelope: ApiEnvelope<T>,
  fallbackMessage: string
): T {
  if (envelope.data === undefined || envelope.data === null) {
    throw new Error(envelope.message || fallbackMessage);
  }

  return envelope.data;
}

export function getUserAccessError(
  error: unknown,
  fallbackMessage = "Unable to complete this action."
) {
  if (!isAxiosError(error)) {
    return error instanceof Error ? error.message : fallbackMessage;
  }

  if (!error.response) {
    return "Unable to reach the server. Check your connection.";
  }

  const responseData = error.response.data;
  if (
    typeof responseData === "object" &&
    responseData !== null &&
    "message" in responseData &&
    typeof responseData.message === "string"
  ) {
    return responseData.message;
  }

  return fallbackMessage;
}

export async function getUsers(query: UserListQuery = {}): Promise<UserPage> {
  const response = await apiClient.get<ApiEnvelope<UserPage>>("/users", {
    params: {
      page: query.page,
      size: query.size,
      search: query.search || undefined,
      role: query.role || undefined,
      status: query.status || undefined,
      includeDeleted: query.includeDeleted || undefined
    }
  });

  return requireData(response.data, "Unable to load users.");
}

export async function getUser(userId: string): Promise<User> {
  const response = await apiClient.get<ApiEnvelope<User>>(`/users/${userId}`);

  return requireData(response.data, "Unable to load user.");
}

export async function createUser(
  request: CreateUserRequest
): Promise<User> {
  const response = await apiClient.post<ApiEnvelope<User>>(
    "/users",
    request
  );

  return requireData(response.data, "Unable to create user.");
}

export async function updateUser(
  userId: string,
  request: UpdateUserRequest
): Promise<User> {
  const response = await apiClient.put<ApiEnvelope<User>>(
    `/users/${userId}`,
    request
  );

  return requireData(response.data, "Unable to update user.");
}

export async function softDeleteUser(userId: string): Promise<User> {
  const response = await apiClient.delete<ApiEnvelope<User>>(`/users/${userId}`);

  return requireData(response.data, "Unable to delete user.");
}

export async function changeUserRole(
  userId: string,
  role: UserRole
): Promise<User> {
  const response = await apiClient.put<ApiEnvelope<User>>(
    `/users/${userId}/role`,
    role,
    {
      headers: {
        "Content-Type": "application/json"
      }
    }
  );

  return requireData(response.data, "Unable to change user role.");
}

export async function changeUserStatus(
  userId: string,
  status: UserStatus
): Promise<User> {
  const response = await apiClient.put<ApiEnvelope<User>>(
    `/users/${userId}/status`,
    status,
    {
      headers: {
        "Content-Type": "application/json"
      }
    }
  );

  return requireData(response.data, "Unable to change user status.");
}

export async function getApplications(): Promise<Application[]> {
  const response =
    await apiClient.get<ApiEnvelope<Application[]>>("/applications");

  return requireData(response.data, "Unable to load applications.");
}

export async function getUserApplicationAccess(
  userId: string
): Promise<ApplicationAccess[]> {
  const response = await apiClient.get<ApiEnvelope<ApplicationAccess[]>>(
    `/users/${userId}/applications`
  );

  return requireData(response.data, "Unable to load application access.");
}

export async function replaceUserApplicationAccess(
  userId: string,
  grants: ApplicationGrant[]
): Promise<ApplicationAccess[]> {
  const response = await apiClient.put<ApiEnvelope<ApplicationAccess[]>>(
    `/users/${userId}/applications`,
    { grants }
  );

  return requireData(response.data, "Unable to update application access.");
}
