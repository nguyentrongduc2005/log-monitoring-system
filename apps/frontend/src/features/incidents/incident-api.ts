import { isAxiosError } from "axios";
import { apiClient } from "@/api/client";
import type { Application } from "@/features/applications/application-types";
import type {
  ApiEnvelope,
  IncidentApplication,
  IncidentDetail,
  IncidentSeverity,
  IncidentStatus,
  IncidentSummary,
  IncidentAnomalyReport,
} from "./incident-types";

function requireData<T>(envelope: ApiEnvelope<T>, fallbackMessage: string): T {
  if (envelope.data === undefined || envelope.data === null) {
    throw new Error(envelope.message || fallbackMessage);
  }
  return envelope.data;
}

export function getIncidentError(
  error: unknown,
  fallbackMessage = "Unable to complete this action.",
) {
  if (!isAxiosError(error)) {
    return error instanceof Error ? error.message : fallbackMessage;
  }

  if (!error.response) {
    return "Unable to reach the server. Check your connection.";
  }

  const data = error.response.data;
  if (
    typeof data === "object" &&
    data !== null &&
    "message" in data &&
    typeof data.message === "string"
  ) {
    return data.message;
  }

  return fallbackMessage;
}

export async function getIncidentApplications(): Promise<IncidentApplication[]> {
  const response = await apiClient.get<ApiEnvelope<Application[]>>(
    "/applications/me",
  );
  return requireData(response.data, "Unable to load applications.").flatMap(
    (application) =>
      application.id
        ? [
            {
              id: application.id,
              name: application.displayName || application.name || "Unnamed application",
            },
          ]
        : [],
  );
}

export async function getIncidents(filters: {
  applicationId?: string;
  status?: "" | IncidentStatus;
  severity?: "" | IncidentSeverity;
}): Promise<IncidentSummary[]> {
  const response = await apiClient.get<ApiEnvelope<IncidentSummary[]>>(
    "/incidents",
    {
      params: {
        applicationId: filters.applicationId || undefined,
        status: filters.status || undefined,
        severity: filters.severity || undefined,
      },
    },
  );
  return requireData(response.data, "Unable to load incidents.");
}

export async function getIncident(id: string): Promise<IncidentDetail> {
  const response = await apiClient.get<ApiEnvelope<IncidentDetail>>(
    `/incidents/${id}`,
  );
  return requireData(response.data, "Unable to load incident.");
}

export async function startIncidentFromAlert(alertId: string): Promise<IncidentDetail> {
  const response = await apiClient.post<ApiEnvelope<IncidentDetail>>(
    `/incidents/from-alert/${alertId}`,
  );
  return requireData(response.data, "Unable to start incident from alert.");
}

export async function resolveIncident(id: string): Promise<IncidentDetail> {
  const response = await apiClient.put<ApiEnvelope<IncidentDetail>>(
    `/incidents/${id}/resolve`,
  );
  return requireData(response.data, "Unable to resolve incident.");
}

export async function getAnomalyReports(): Promise<IncidentAnomalyReport[]> {
  const response = await apiClient.get<ApiEnvelope<IncidentAnomalyReport[]>>(
    "/incidents/anomaly-reports",
  );
  return requireData(response.data, "Unable to load anomaly reports.");
}

export async function getAnomalyReport(id: string): Promise<IncidentAnomalyReport> {
  const response = await apiClient.get<ApiEnvelope<IncidentAnomalyReport>>(
    `/incidents/anomaly-reports/${id}`,
  );
  return requireData(response.data, "Unable to load anomaly report.");
}
