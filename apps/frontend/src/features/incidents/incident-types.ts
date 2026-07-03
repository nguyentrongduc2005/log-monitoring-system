import type { ApiEnvelope } from "@/features/applications/application-types";

export type { ApiEnvelope };

export type IncidentStatus = "INVESTIGATING" | "MITIGATED" | "RESOLVED";
export type IncidentSeverity = "SEV1" | "SEV2" | "SEV3" | "UNKNOWN";
export type IncidentScope = "APPLICATION" | "MULTI_APPLICATION" | "SYSTEM_WIDE";

export type IncidentApplication = {
  id: string;
  name: string;
};

export type IncidentSummary = {
  id: string;
  title: string;
  description?: string | null;
  shortSummary: string;
  impact: string;
  status: IncidentStatus;
  severity: IncidentSeverity;
  scope: IncidentScope;
  triggerType: "ALERT";
  startedAt: string;
  windowStart: string;
  windowEnd: string;
  lastEvidenceCollectedAt?: string | null;
  applicationIds: string[];
  createdBy: string;
  resolvedBy?: string | null;
  resolvedAt?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type IncidentApplicationImpact = {
  applicationId: string;
  impactRole: "PRIMARY" | "RELATED" | "SUSPECTED";
  createdAt: string;
};

export type IncidentEvidence = {
  id: string;
  type: string;
  sourceId?: string | null;
  applicationId: string;
  fingerprint?: string | null;
  severity?: string | null;
  summary: string;
  sampleMessage?: string | null;
  metadataJson?: string | null;
  occurredAt: string;
};

export type IncidentTimelineEvent = {
  id: string;
  eventType: string;
  message: string;
  actorUserId?: string | null;
  metadataJson?: string | null;
  createdAt: string;
};

export type IncidentDetail = Omit<IncidentSummary, "applicationIds"> & {
  possibleCause?: string | null;
  recommendedActions: string[];
  applications: IncidentApplicationImpact[];
  evidence: IncidentEvidence[];
  timeline: IncidentTimelineEvent[];
};

export type IncidentAnomalyReport = {
  id: string;
  alertId: string;
  status: string;
  evidencePayload: string;
  createdAt: string;
  updatedAt: string;
};
