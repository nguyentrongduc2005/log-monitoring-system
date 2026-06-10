import type { OverviewSnapshot } from "@/features/dashboard/overview-types";

const overviewSnapshot: OverviewSnapshot = {
  metrics: [
    {
      id: "accepted",
      label: "Logs accepted/min",
      value: "14,820",
      trend: "+8.4%",
      tone: "success"
    },
    {
      id: "error-rate",
      label: "Error rate",
      value: "2.4%",
      trend: "-0.3%",
      tone: "warning"
    },
    {
      id: "critical-alerts",
      label: "Critical alerts",
      value: "3",
      trend: "+1",
      tone: "error"
    },
    {
      id: "active-apps",
      label: "Active applications",
      value: "12",
      tone: "neutral"
    },
    {
      id: "lag",
      label: "Processing lag",
      value: "380 ms",
      trend: "-45 ms",
      tone: "success"
    }
  ],
  pipeline: [
    {
      id: "ingestion-api",
      label: "Ingestion API",
      state: "healthy",
      detail: "HTTP ingress is accepting new batches."
    },
    {
      id: "kafka-raw",
      label: "Kafka logs.raw",
      state: "healthy",
      detail: "Backpressure remains within the safe threshold."
    },
    {
      id: "worker",
      label: "Worker",
      state: "delayed",
      detail: "Normalization is 380 ms behind the ingress edge."
    },
    {
      id: "clickhouse",
      label: "ClickHouse",
      state: "healthy",
      detail: "Recent inserts completed without storage errors."
    },
    {
      id: "websocket",
      label: "WebSocket",
      state: "healthy",
      detail: "Live monitoring clients are receiving updates."
    },
    {
      id: "alerting",
      label: "Alerting / Telegram",
      state: "degraded",
      detail: "Alert delivery is slower than the last five-minute baseline."
    }
  ],
  volume: [
    { time: "09:55", INFO: 180, WARN: 20, ERROR: 10, CRITICAL: 2 },
    { time: "10:00", INFO: 220, WARN: 24, ERROR: 11, CRITICAL: 2 },
    { time: "10:05", INFO: 240, WARN: 26, ERROR: 14, CRITICAL: 3 },
    { time: "10:10", INFO: 210, WARN: 23, ERROR: 13, CRITICAL: 2 },
    { time: "10:15", INFO: 260, WARN: 29, ERROR: 15, CRITICAL: 4 }
  ],
  noisyApplications: [
    {
      id: "checkout-api",
      name: "checkout-api",
      environment: "production",
      totalLogs: 6240,
      errorCount: 132,
      criticalCount: 8,
      errorRate: "2.2%",
      lastSeen: "10:15 UTC"
    },
    {
      id: "billing-worker",
      name: "billing-worker",
      environment: "production",
      totalLogs: 4512,
      errorCount: 104,
      criticalCount: 6,
      errorRate: "2.4%",
      lastSeen: "10:14 UTC"
    },
    {
      id: "identity-service",
      name: "identity-service",
      environment: "staging",
      totalLogs: 2380,
      errorCount: 32,
      criticalCount: 1,
      errorRate: "1.3%",
      lastSeen: "10:12 UTC"
    }
  ],
  criticalAlerts: [
    {
      id: "alert-1",
      severity: "CRITICAL",
      application: "checkout-api",
      fingerprint: "PAYMENT_GATEWAY_TIMEOUT",
      message: "Payment gateway timeout crossed alert threshold.",
      occurrences: 28,
      lastSeen: "10:15 UTC",
      deliveryState: "Delivered"
    },
    {
      id: "alert-2",
      severity: "ERROR",
      application: "billing-worker",
      fingerprint: "INVOICE_RETRY_STORM",
      message: "Invoice retries are climbing faster than the worker drain rate.",
      occurrences: 19,
      lastSeen: "10:13 UTC",
      deliveryState: "Deduplicated"
    }
  ],
  demoReadiness: {
    accepted: 500,
    rejected: 0,
    duration: "2.0s",
    p95AckLatency: "84 ms",
    streamStatus: "Smooth",
    buffered: 8,
    dropped: 0
  },
  authorizedApplications: 12
};

export async function getOverviewSnapshot(): Promise<OverviewSnapshot> {
  return Promise.resolve(overviewSnapshot);
}
