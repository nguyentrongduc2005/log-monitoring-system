export type RetentionLogLevel = "INFO" | "WARN" | "ERROR" | "CRITICAL";

export type RetentionOperationStatus = "SUCCESS" | "FAILED" | "PENDING";

export type RetentionJob = {
  id: string;
  logLevel: RetentionLogLevel;
  label: string;
  description: string;
  retentionDays: number;
  minDays: number;
  maxDays: number;
  enabled: boolean;
  nextRunAt: string;
  recentOperation: RetentionRun | null;
};

export type RetentionRun = {
  id: string;
  policyId: string;
  status: RetentionOperationStatus;
  message: string;
  startedAt: string;
  finishedAt: string | null;
  affectedRows: number;
};

export type RetentionJobDraft = Pick<RetentionJob, "id" | "retentionDays" | "enabled">;
