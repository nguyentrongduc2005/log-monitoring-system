export type LogLevel = "INFO" | "WARN" | "ERROR" | "CRITICAL";

export type DashboardWindow = "15m" | "1h" | "6h" | "24h";

export type PipelineState =
  | "healthy"
  | "degraded"
  | "delayed"
  | "offline"
  | "unknown";

export type OverviewMetric = {
  id: string;
  label: string;
  value: string;
  trend?: string;
  helper?: string;
  tone: "neutral" | "success" | "warning" | "error";
};

export type PipelineStep = {
  id: string;
  label: string;
  state: PipelineState;
  detail: string;
};

export type LogVolumePoint = {
  time: string;
  INFO: number;
  WARN: number;
  ERROR: number;
  CRITICAL: number;
};

export type NoisyApplication = {
  id: string;
  name: string;
  environment: string;
  totalLogs: number;
  errorCount: number;
  criticalCount: number;
  errorRate: string;
  lastSeen: string;
};

export type CriticalAlertSummary = {
  id: string;
  severity: "ERROR" | "CRITICAL";
  application: string;
  logSamples?: { level: string; message: string }[];
  occurrences: number;
  lastSeen: string;
  deliveryState: string;
};

export type NotificationSummary = {
  sent: number;
  failed: number;
  dedupSuppressed: number;
  deliveryRate: string;
  lastFailure: string;
};

export type LogLevelDistribution = {
  level: LogLevel;
  count: number;
  percentage: number;
};

export type OverviewSnapshot = {
  window: string;
  generatedAt: string;
  metrics: OverviewMetric[];
  pipeline: PipelineStep[];
  volume: LogVolumePoint[];
  levelDistribution: LogLevelDistribution[];
  noisyApplications: NoisyApplication[];
  criticalAlerts: CriticalAlertSummary[];
  notificationSummary: NotificationSummary;
  authorizedApplications: number;
};
