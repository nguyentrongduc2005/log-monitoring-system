export type LogLevel = "INFO" | "WARN" | "ERROR" | "CRITICAL";

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
  fingerprint: string;
  message: string;
  occurrences: number;
  lastSeen: string;
  deliveryState: string;
};

export type DemoReadiness = {
  accepted: number;
  rejected: number;
  duration: string;
  p95AckLatency: string;
  streamStatus: string;
  buffered: number;
  dropped: number;
};

export type OverviewSnapshot = {
  metrics: OverviewMetric[];
  pipeline: PipelineStep[];
  volume: LogVolumePoint[];
  noisyApplications: NoisyApplication[];
  criticalAlerts: CriticalAlertSummary[];
  demoReadiness: DemoReadiness;
  authorizedApplications: number;
};
