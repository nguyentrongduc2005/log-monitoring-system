import { apiClient } from "@/api/client";
import type { ApiEnvelope } from "@/features/alert-rules/alert-rules-types";
import type {
  RetentionJob,
  RetentionJobDraft,
  RetentionRun
} from "./retention-types";

function requireData<T>(envelope: ApiEnvelope<T>, fallbackMessage: string): T {
  if (envelope.data === undefined || envelope.data === null) {
    throw new Error(envelope.message || fallbackMessage);
  }
  return envelope.data;
}

export async function getRetentionJobs(): Promise<RetentionJob[]> {
  const response =
    await apiClient.get<ApiEnvelope<RetentionJob[]>>("/retention/policies");
  return requireData(response.data, "Unable to load retention policies.");
}

export async function saveRetentionJobs(
  drafts: RetentionJobDraft[]
): Promise<RetentionJob[]> {
  const response = await apiClient.put<ApiEnvelope<RetentionJob[]>>(
    "/retention/policies",
    drafts
  );
  return requireData(response.data, "Unable to save retention policies.");
}

export async function runRetentionJob(policyId: string): Promise<RetentionRun> {
  const response = await apiClient.post<ApiEnvelope<RetentionRun>>(
    `/retention/policies/${policyId}/run`
  );
  return requireData(response.data, "Unable to run retention policy.");
}
