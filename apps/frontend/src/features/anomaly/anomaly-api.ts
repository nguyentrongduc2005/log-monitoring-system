import { apiClient } from "@/api/client";
import type { ApiEnvelope, AnomalyReport } from "./anomaly-types";

function requireData<T>(envelope: ApiEnvelope<T>, fallbackMessage: string): T {
  if (envelope.data === undefined || envelope.data === null) {
    throw new Error(envelope.message || fallbackMessage);
  }
  return envelope.data;
}

export async function getAnomalyReports(applicationId?: string): Promise<AnomalyReport[]> {
  const response = await apiClient.get<ApiEnvelope<AnomalyReport[]>>(
    "/anomaly/reports",
    {
      params: {
        applicationId: applicationId || undefined,
      },
    }
  );
  return requireData(response.data, "Unable to load anomaly reports.");
}

export async function getAnomalyReport(id: string): Promise<AnomalyReport> {
  const response = await apiClient.get<ApiEnvelope<AnomalyReport>>(
    `/anomaly/reports/${id}`
  );
  return requireData(response.data, "Unable to load anomaly report.");
}

export async function resolveAnomalyReport(id: string): Promise<AnomalyReport> {
  const response = await apiClient.put<ApiEnvelope<AnomalyReport>>(
    `/anomaly/reports/${id}/resolve`
  );
  return requireData(response.data, "Unable to resolve anomaly report.");
}
