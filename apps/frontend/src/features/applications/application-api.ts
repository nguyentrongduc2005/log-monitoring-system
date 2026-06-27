import { apiClient } from "@/api/client";
import { isAxiosError } from "axios";
import type {
  ApiEnvelope,
  ApiKey,
  ApiKeyCreation,
  Application,
  ApplicationRequest,
  ApplicationStatus,
  CreateApiKeyRequest
} from "./application-types";

function requireData<T>(
  envelope: ApiEnvelope<T>,
  fallbackMessage: string
): T {
  if (envelope.data === undefined || envelope.data === null) {
    throw new Error(envelope.message || fallbackMessage);
  }

  return envelope.data;
}

export function getApplicationError(
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

export async function getApplications(): Promise<Application[]> {
  const response =
    await apiClient.get<ApiEnvelope<Application[]>>("/applications");

  return requireData(response.data, "Unable to load applications.");
}

export async function createApplication(
  request: ApplicationRequest
): Promise<Application> {
  const response = await apiClient.post<ApiEnvelope<Application>>(
    "/applications",
    request
  );

  return requireData(response.data, "Unable to create application.");
}

export async function updateApplication(
  applicationId: string,
  request: ApplicationRequest
): Promise<Application> {
  const response = await apiClient.put<ApiEnvelope<Application>>(
    `/applications/${applicationId}`,
    request
  );

  return requireData(response.data, "Unable to update application.");
}

export async function changeApplicationStatus(
  applicationId: string,
  status: ApplicationStatus
): Promise<Application> {
  const response = await apiClient.put<ApiEnvelope<Application>>(
    `/applications/${applicationId}/status`,
    { status }
  );

  return requireData(response.data, "Unable to change application status.");
}

export async function getApplicationApiKeys(
  applicationId: string
): Promise<ApiKey[]> {
  const response = await apiClient.get<ApiEnvelope<ApiKey[]>>(
    `/applications/${applicationId}/api-keys`
  );

  return requireData(response.data, "Unable to load API keys.");
}

export async function createApiKey(
  applicationId: string,
  request: CreateApiKeyRequest
): Promise<ApiKeyCreation> {
  const response = await apiClient.post<ApiEnvelope<ApiKeyCreation>>(
    `/applications/${applicationId}/api-keys`,
    request
  );

  return requireData(response.data, "Unable to create API key.");
}

export async function rotateApiKey(
  applicationId: string,
  apiKeyId: string
): Promise<ApiKeyCreation> {
  const response = await apiClient.post<ApiEnvelope<ApiKeyCreation>>(
    `/applications/${applicationId}/api-keys/${apiKeyId}/rotate`
  );

  return requireData(response.data, "Unable to rotate API key.");
}

export async function revokeApiKey(
  applicationId: string,
  apiKeyId: string
): Promise<void> {
  await apiClient.post<ApiEnvelope<void>>(
    `/applications/${applicationId}/api-keys/${apiKeyId}/revoke`
  );
}

export async function getMetricSource(
  applicationId: string
): Promise<import("./application-types").MetricSource | null> {
  const response = await apiClient.get<import("./application-types").MetricSource>(
    `/applications/${applicationId}/metric-sources`
  );

  return response.status === 204 ? null : response.data;
}

export async function saveMetricSource(
  applicationId: string,
  request: import("./application-types").MetricSourceRequest
): Promise<import("./application-types").MetricSource> {
  const response = await apiClient.post<import("./application-types").MetricSource>(
    `/applications/${applicationId}/metric-sources`,
    request
  );

  return response.data;
}

export async function updateMetricSource(
  applicationId: string,
  request: import("./application-types").MetricSourceRequest
): Promise<import("./application-types").MetricSource> {
  const response = await apiClient.put<import("./application-types").MetricSource>(
    `/metric-sources/${applicationId}`,
    request
  );

  return response.data;
}

export async function testMetricSourceConnection(
  request: import("./application-types").MetricSourceRequest
): Promise<boolean> {
  const response = await apiClient.post<boolean>(
    `/metric-sources/test-connection`,
    request
  );

  return response.data;
}
