import type {
  DashboardWindow,
  LogVolumePoint,
  OverviewSnapshot,
} from "@/features/dashboard/overview-types";

const windowLabels: Record<DashboardWindow, string> = {
  "15m": "Last 15 minutes",
  "1h": "Last 1 hour",
  "6h": "Last 6 hours",
  "24h": "Last 24 hours",
};

const mockLogVolumeByWindow: Record<DashboardWindow, LogVolumePoint[]> = {
  "15m": [
    { time: "10:15", INFO: 940, WARN: 108, ERROR: 34, CRITICAL: 3 },
    { time: "10:18", INFO: 1020, WARN: 142, ERROR: 45, CRITICAL: 4 },
    { time: "10:21", INFO: 1110, WARN: 148, ERROR: 52, CRITICAL: 6 },
    { time: "10:24", INFO: 980, WARN: 121, ERROR: 39, CRITICAL: 4 },
    { time: "10:27", INFO: 1250, WARN: 166, ERROR: 68, CRITICAL: 7 },
    { time: "10:30", INFO: 1180, WARN: 152, ERROR: 61, CRITICAL: 5 },
  ],
  "1h": [
    { time: "09:30", INFO: 3820, WARN: 420, ERROR: 126, CRITICAL: 9 },
    { time: "09:40", INFO: 4160, WARN: 510, ERROR: 148, CRITICAL: 12 },
    { time: "09:50", INFO: 3980, WARN: 476, ERROR: 132, CRITICAL: 10 },
    { time: "10:00", INFO: 4550, WARN: 620, ERROR: 210, CRITICAL: 18 },
    { time: "10:10", INFO: 4890, WARN: 650, ERROR: 248, CRITICAL: 21 },
    { time: "10:20", INFO: 4720, WARN: 602, ERROR: 226, CRITICAL: 17 },
    { time: "10:30", INFO: 5160, WARN: 710, ERROR: 302, CRITICAL: 26 },
  ],
  "6h": [
    { time: "04:30", INFO: 14200, WARN: 1300, ERROR: 380, CRITICAL: 18 },
    { time: "05:30", INFO: 15840, WARN: 1510, ERROR: 420, CRITICAL: 22 },
    { time: "06:30", INFO: 14990, WARN: 1400, ERROR: 390, CRITICAL: 19 },
    { time: "07:30", INFO: 17250, WARN: 1880, ERROR: 610, CRITICAL: 35 },
    { time: "08:30", INFO: 18480, WARN: 2100, ERROR: 740, CRITICAL: 42 },
    { time: "09:30", INFO: 19220, WARN: 2260, ERROR: 820, CRITICAL: 48 },
    { time: "10:30", INFO: 20100, WARN: 2440, ERROR: 930, CRITICAL: 54 },
  ],
  "24h": [
    { time: "Tue 12", INFO: 68400, WARN: 6200, ERROR: 1800, CRITICAL: 92 },
    { time: "Tue 15", INFO: 71200, WARN: 7100, ERROR: 2100, CRITICAL: 110 },
    { time: "Tue 18", INFO: 74500, WARN: 7800, ERROR: 2380, CRITICAL: 126 },
    { time: "Tue 21", INFO: 70100, WARN: 6900, ERROR: 1980, CRITICAL: 104 },
    { time: "Wed 00", INFO: 62200, WARN: 5100, ERROR: 1320, CRITICAL: 70 },
    { time: "Wed 03", INFO: 58100, WARN: 4500, ERROR: 1040, CRITICAL: 48 },
    { time: "Wed 06", INFO: 63900, WARN: 5900, ERROR: 1660, CRITICAL: 82 },
    { time: "Wed 09", INFO: 76900, WARN: 8600, ERROR: 2840, CRITICAL: 142 },
  ],
};

const overviewSnapshot: OverviewSnapshot = {
  window: windowLabels["15m"],
  generatedAt: "2026-06-23T10:30:00Z",
  metrics: [
    {
      id: "logs-per-minute",
      label: "Logs/min",
      value: "1,228",
      trend: "+8.4%",
      helper: "18,420 logs in scope",
      tone: "success",
    },
    {
      id: "error-rate",
      label: "Error rate",
      value: "3.7%",
      trend: "+0.8%",
      helper: "ERROR and CRITICAL",
      tone: "warning",
    },
    {
      id: "critical-alerts",
      label: "Open critical",
      value: "4",
      trend: "+2",
      helper: "Needs acknowledgement",
      tone: "error",
    },
    {
      id: "active-apps",
      label: "Active applications",
      value: "8 / 12",
      helper: "Sending logs recently",
      tone: "neutral",
    },
    {
      id: "lag",
      label: "Processing lag",
      value: "1,250",
      trend: "+310",
      helper: "raw Kafka messages",
      tone: "warning",
    },
    {
      id: "notification-failures",
      label: "Delivery failures",
      value: "3",
      trend: "-1",
      helper: "Telegram failures",
      tone: "warning",
    },
  ],
  pipeline: [
    {
      id: "ingestion-api",
      label: "Ingestion API",
      state: "healthy",
      detail: "HTTP ingest is accepting batches.",
    },
    {
      id: "kafka-raw",
      label: "Kafka logs.raw",
      state: "delayed",
      detail: "Consumer lag is above the 15-minute baseline.",
    },
    {
      id: "worker",
      label: "Processing",
      state: "delayed",
      detail: "Normalization is draining, but slower than ingress.",
    },
    {
      id: "clickhouse",
      label: "ClickHouse",
      state: "healthy",
      detail: "Recent inserts completed without storage errors.",
    },
    {
      id: "websocket",
      label: "WebSocket",
      state: "healthy",
      detail: "Live monitoring clients are receiving updates.",
    },
    {
      id: "alerting",
      label: "Alerting",
      state: "degraded",
      detail: "Telegram delivery has recent failures.",
    },
  ],
  volume: mockLogVolumeByWindow["15m"],
  levelDistribution: [
    { level: "INFO", count: 6480, percentage: 82 },
    { level: "WARN", count: 837, percentage: 11 },
    { level: "ERROR", count: 299, percentage: 6 },
    { level: "CRITICAL", count: 29, percentage: 1 },
  ],
  noisyApplications: [
    {
      id: "checkout-api",
      name: "Checkout API",
      environment: "production",
      totalLogs: 5200,
      errorCount: 310,
      criticalCount: 8,
      errorRate: "5.9%",
      lastSeen: "10:29 UTC",
    },
    {
      id: "billing-worker",
      name: "Billing Worker",
      environment: "production",
      totalLogs: 4512,
      errorCount: 188,
      criticalCount: 6,
      errorRate: "4.2%",
      lastSeen: "10:27 UTC",
    },
    {
      id: "identity-service",
      name: "Identity Service",
      environment: "staging",
      totalLogs: 2380,
      errorCount: 74,
      criticalCount: 2,
      errorRate: "3.1%",
      lastSeen: "10:25 UTC",
    },
  ],
  criticalAlerts: [
    {
      id: "alert-1",
      severity: "CRITICAL",
      application: "Checkout API",
      logSamples: [{ level: "CRITICAL", message: "Payment gateway timeout crossed alert threshold." }],
      occurrences: 28,
      lastSeen: "10:29 UTC",
      deliveryState: "Delivered",
    },
    {
      id: "alert-2",
      severity: "CRITICAL",
      application: "Billing Worker",
      logSamples: [{ level: "CRITICAL", message: "Invoice retries are climbing faster than the worker drain rate." }],
      occurrences: 19,
      lastSeen: "10:27 UTC",
      deliveryState: "Deduplicated",
    },
    {
      id: "alert-3",
      severity: "ERROR",
      application: "Identity Service",
      logSamples: [{ level: "ERROR", message: "Refresh token rejection rate exceeded the warning threshold." }],
      occurrences: 11,
      lastSeen: "10:24 UTC",
      deliveryState: "Delivered",
    },
  ],
  notificationSummary: {
    sent: 32,
    failed: 3,
    dedupSuppressed: 128,
    deliveryRate: "91.4%",
    lastFailure: "10:23 UTC",
  },
  authorizedApplications: 12,
};

export async function getOverviewSnapshot(
  window: DashboardWindow = "15m",
): Promise<OverviewSnapshot> {
  return Promise.resolve({
    ...overviewSnapshot,
    window: windowLabels[window],
    volume: mockLogVolumeByWindow[window],
  });
}
