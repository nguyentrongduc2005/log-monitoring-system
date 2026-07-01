export type LogLevel = "INFO" | "WARN" | "ERROR" | "CRITICAL";

export type DashboardWindow = "15m" | "1h" | "6h" | "24h";

export type OverviewMetric = {
  id: string;
  label: string;
  value: string;
  trend?: string;
  helper?: string;
  tone: "neutral" | "success" | "warning" | "error";
};

export type LogVolumePoint = {
  time: string;
  INFO: number;
  WARN: number;
  ERROR: number;
  CRITICAL: number;
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

export type OverviewSnapshot = {
  window: string;
  generatedAt: string;
  metrics: OverviewMetric[];
  volume: LogVolumePoint[];
  criticalAlerts: CriticalAlertSummary[];
};
