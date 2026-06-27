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

export type IncidentErrorLog = {
  eventId: string;
  applicationId?: string | null;
  applicationName?: string | null;
  applicationDisplayName?: string | null;
  level: string;
  message: string;

  traceId?: string | null;
  logTimestamp: string;
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
  errorLogs: IncidentErrorLog[];
  timeline: IncidentTimelineEvent[];
};
