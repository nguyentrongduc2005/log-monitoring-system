import type { components } from "@/api/generated/api-types";

export type User = components["schemas"]["UserResponse"];
export type CreateUserRequest = components["schemas"]["CreateUserRequest"];
export type UpdateUserRequest = {
  email?: string;
  displayName?: string;
};
export type Application = components["schemas"]["ApplicationResponse"];
export type ApplicationAccess =
  components["schemas"]["ApplicationAccessResponse"];

export type UserRole = "ADMIN" | "ENGINEER";

export type UserStatus = "ACTIVE" | "DISABLED" | "LOCKED" | "DELETED";

export type UserListQuery = {
  page?: number;
  size?: number;
  search?: string;
  role?: UserRole | "";
  status?: UserStatus | "";
  includeDeleted?: boolean;
};

export type UserPage = {
  users: User[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type AccessLevel = "VIEW" | "MANAGE";
export type AccessSelection = AccessLevel | "NONE";

export type ApplicationGrant = {
  applicationId: string;
  accessLevel: AccessLevel;
};

export type ApiEnvelope<T> = {
  success?: boolean;
  message?: string;
  data?: T;
  timestamp?: string;
};
