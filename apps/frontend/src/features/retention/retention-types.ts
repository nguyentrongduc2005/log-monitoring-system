export type RetentionLogLevel = "INFO" | "WARN" | "ERROR" | "CRITICAL";

export type RetentionAction = "DELETE" | "COMPRESS" | "ARCHIVE";

export type RetentionOperationStatus = "SUCCESS" | "WARNING";

export type RetentionJob = {
  id: string;
  logLevel: RetentionLogLevel;
  label: string;
  description: string;
  retentionDays: number;
  action: RetentionAction;
  minDays: number;
  maxDays: number;
  storageTb: number;
  storagePercent: number;
  projectedDeletionTbPerMonth: number;
  compressionSavingsGbPerMonth: number;
  nextRunAt: string;
  recentOperation: {
    status: RetentionOperationStatus;
    message: string;
    occurredAt: string;
  };
};

export type RetentionJobDraft = Pick<RetentionJob, "id" | "retentionDays" | "action">;
