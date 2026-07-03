export type HealthWindow = "15m" | "1h" | "6h" | "24h";

export type HealthStatus = "HEALTHY" | "DEGRADED" | "CRITICAL" | "NO_DATA";

export type HealthMetric = {
  id: string;
  label: string;
  value: string;
  helper?: string;
  tone: "neutral" | "success" | "warning" | "error";
};

export type ApplicationErrorRatePoint = {
  time: string;
  rates: Record<string, number>;
};

export type ApplicationStability = {
  applicationId: string;
  applicationName: string;
  avgErrorRate: number;
  peakErrorRate: number;
  criticalLogs: number;
  openAlerts: number;
  status: Exclude<HealthStatus, "NO_DATA">;
  detail: {
    totalLogs: number;
    errorLogs: number;
    warningLogs: number;
    peakHour: string;
    lastEvaluated: string;
    topIssue: string;
    insight: string;
  };
};

export type HealthTrendPoint = {
  time: string;
  totalLogs: number;
  errorRate: number;
  criticalAlerts: number;
};

export type SeverityBreakdownItem = {
  level: "INFO" | "WARN" | "ERROR" | "CRITICAL";
  count: number;
  percentage: number;
};

export type ErrorFingerprint = {
  id: string;
  fingerprint: string;
  message: string;
  severity: "ERROR" | "CRITICAL";
  occurrences: number;
  firstSeen: string;
  lastSeen: string;
  trend: "Increasing" | "Stable" | "Decreasing";
};

export type ApplicationAlert = {
  id: string;
  severity: "ERROR" | "CRITICAL";
  ruleName: string;
  logSamples?: { level: string; message: string }[];
  occurrences: number;
  status: "OPEN" | "ACKNOWLEDGED" | "RESOLVED";
  lastSeen: string;
};

export type ProblemLogSample = {
  id: string;
  timestamp: string;
  level: "ERROR" | "CRITICAL";
  fingerprint: string;
  message: string;
  traceId?: string;
};

export type ApplicationOption = {
  id: string;
  name: string;
};

export type ApplicationHealthSnapshot = {
  application: ApplicationOption;
  applications: ApplicationOption[];
  window: string;
  generatedAt: string;
  comparisonSummary: HealthMetric[];
  hourlyErrorRates: ApplicationErrorRatePoint[];
  stabilityRanking: ApplicationStability[];
  healthScore: number;
  healthStatus: HealthStatus;
  healthReason: string;
  metrics: HealthMetric[];
  trend: HealthTrendPoint[];
  severityBreakdown: SeverityBreakdownItem[];
  topFingerprints: ErrorFingerprint[];
  recentAlerts: ApplicationAlert[];
  problemLogs: ProblemLogSample[];
};
