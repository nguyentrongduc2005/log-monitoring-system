import type { ApiEnvelope } from "@/features/applications/application-types";

export type { ApiEnvelope };

export type AnomalyReport = {
  id: string;
  applicationId: string;
  alertId?: string | null;
  sourceType: string;
  ruleName: string;
  fingerprint: string;
  severity: string;
  status: string;
  title: string;
  summary?: string | null;
  hypothesis?: string | null;
  confidenceScore?: number | null;
  windowStart: string;
  windowEnd: string;
  occurrenceCount: number;
  firstSeenAt: string;
  lastSeenAt: string;
  evidencePayloadJson: string;
  aiTriggerRequested: boolean;
  aiTriggerReason?: string | null;
  aiStatus: string;
  aiStartedAt?: string | null;
  aiCompletedAt?: string | null;
  aiResultJson?: string | null;
  aiError?: string | null;
  resolvedBy?: string | null;
  resolvedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};
