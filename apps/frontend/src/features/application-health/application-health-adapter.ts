import type {
  ApplicationErrorRatePoint,
  ApplicationHealthSnapshot,
  ApplicationOption,
  ApplicationStability,
  HealthMetric,
  HealthWindow,
} from "./application-health-types";

const windowLabels: Record<HealthWindow, string> = {
  "15m": "Last 15 minutes",
  "1h": "Last 1 hour",
  "6h": "Last 6 hours",
  "24h": "Last 24 hours",
};

const applications: ApplicationOption[] = [
  { id: "checkout-api", name: "Checkout API" },
  { id: "billing-worker", name: "Billing Worker" },
  { id: "identity-service", name: "Identity Service" },
];

const comparisonSummary: HealthMetric[] = [
  {
    id: "tracked-apps",
    label: "Tracked apps",
    value: "3",
    helper: "Applications with logs",
    tone: "neutral",
  },
  {
    id: "worst-app",
    label: "Least stable",
    value: "Checkout API",
    helper: "Avg error rate 4.5%",
    tone: "error",
  },
  {
    id: "peak-error-rate",
    label: "Peak error rate",
    value: "5.9%",
    helper: "Checkout API at 10:30",
    tone: "error",
  },
  {
    id: "open-alerts",
    label: "Open alerts",
    value: "7",
    helper: "Across compared apps",
    tone: "warning",
  },
];

const hourlyErrorRates: ApplicationErrorRatePoint[] = [
  {
    time: "10:00",
    rates: {
      "checkout-api": 2.8,
      "billing-worker": 2.1,
      "identity-service": 1.9,
    },
  },
  {
    time: "10:05",
    rates: {
      "checkout-api": 3.2,
      "billing-worker": 2.4,
      "identity-service": 2.1,
    },
  },
  {
    time: "10:10",
    rates: {
      "checkout-api": 3.9,
      "billing-worker": 2.8,
      "identity-service": 2.4,
    },
  },
  {
    time: "10:15",
    rates: {
      "checkout-api": 3.4,
      "billing-worker": 2.8,
      "identity-service": 2.9,
    },
  },
  {
    time: "10:20",
    rates: {
      "checkout-api": 4.4,
      "billing-worker": 3.5,
      "identity-service": 3.2,
    },
  },
  {
    time: "10:25",
    rates: {
      "checkout-api": 5.1,
      "billing-worker": 3.9,
      "identity-service": 3.1,
    },
  },
  {
    time: "10:30",
    rates: {
      "checkout-api": 5.9,
      "billing-worker": 4.2,
      "identity-service": 3.1,
    },
  },
];

const stabilityRanking: ApplicationStability[] = [
  {
    applicationId: "checkout-api",
    applicationName: "Checkout API",
    avgErrorRate: 4.5,
    peakErrorRate: 5.9,
    criticalLogs: 8,
    openAlerts: 4,
    status: "DEGRADED",
    detail: {
      totalLogs: 5200,
      errorLogs: 310,
      warningLogs: 322,
      peakHour: "10:30",
      lastEvaluated: "10:30 UTC",
      topIssue: "PAYMENT_GATEWAY_TIMEOUT",
      insight:
        "Error rate is rising near payment gateway calls; this app is currently the least stable service.",
    },
  },
  {
    applicationId: "billing-worker",
    applicationName: "Billing Worker",
    avgErrorRate: 3.1,
    peakErrorRate: 4.2,
    criticalLogs: 6,
    openAlerts: 2,
    status: "DEGRADED",
    detail: {
      totalLogs: 4512,
      errorLogs: 188,
      warningLogs: 418,
      peakHour: "10:30",
      lastEvaluated: "10:30 UTC",
      topIssue: "INVOICE_RETRY_STORM",
      insight:
        "Retry pressure is elevated, but the peak error rate remains lower than Checkout API.",
    },
  },
  {
    applicationId: "identity-service",
    applicationName: "Identity Service",
    avgErrorRate: 2.7,
    peakErrorRate: 3.2,
    criticalLogs: 2,
    openAlerts: 1,
    status: "HEALTHY",
    detail: {
      totalLogs: 2380,
      errorLogs: 74,
      warningLogs: 194,
      peakHour: "10:20",
      lastEvaluated: "10:30 UTC",
      topIssue: "JWT_REFRESH_REJECTED",
      insight:
        "Authentication failures are present but stable, with no current error-rate spike.",
    },
  },
];

const baseSnapshots: Record<
  string,
  Omit<ApplicationHealthSnapshot, "window">
> = {
  "checkout-api": {
    application: applications[0],
    applications,
    generatedAt: "2026-06-23T10:30:00Z",
    comparisonSummary,
    hourlyErrorRates,
    stabilityRanking,
    healthScore: 62,
    healthStatus: "DEGRADED",
    healthReason:
      "Payment timeout fingerprint is increasing and open critical alerts need acknowledgement.",
    metrics: [
      {
        id: "total-logs",
        label: "Total logs",
        value: "5,200",
        helper: "Current window",
        tone: "neutral",
      },
      {
        id: "error-rate",
        label: "Error rate",
        value: "5.9%",
        helper: "+1.4% vs previous",
        tone: "warning",
      },
      {
        id: "critical-logs",
        label: "Critical logs",
        value: "8",
        helper: "Payment path",
        tone: "error",
      },
      {
        id: "open-alerts",
        label: "Open alerts",
        value: "4",
        helper: "2 critical",
        tone: "error",
      },
      {
        id: "fingerprints",
        label: "Fingerprints",
        value: "17",
        helper: "Unique error groups",
        tone: "warning",
      },
      {
        id: "last-seen",
        label: "Last seen",
        value: "10:29",
        helper: "Receiving logs",
        tone: "success",
      },
    ],
    trend: [
      { time: "10:15", totalLogs: 1085, errorRate: 3.4, criticalAlerts: 1 },
      { time: "10:18", totalLogs: 1211, errorRate: 4.0, criticalAlerts: 1 },
      { time: "10:21", totalLogs: 1316, errorRate: 4.5, criticalAlerts: 2 },
      { time: "10:24", totalLogs: 1144, errorRate: 3.8, criticalAlerts: 2 },
      { time: "10:27", totalLogs: 1491, errorRate: 5.1, criticalAlerts: 3 },
      { time: "10:30", totalLogs: 1398, errorRate: 5.9, criticalAlerts: 4 },
    ],
    severityBreakdown: [
      { level: "INFO", count: 4560, percentage: 88 },
      { level: "WARN", count: 322, percentage: 6 },
      { level: "ERROR", count: 310, percentage: 5 },
      { level: "CRITICAL", count: 8, percentage: 1 },
    ],
    topFingerprints: [
      {
        id: "fp-payment-timeout",
        fingerprint: "PAYMENT_GATEWAY_TIMEOUT",
        message: "Payment gateway timeout after 3000ms.",
        severity: "CRITICAL",
        occurrences: 28,
        firstSeen: "10:03",
        lastSeen: "10:29",
        trend: "Increasing",
      },
      {
        id: "fp-cart-lock",
        fingerprint: "CART_LOCK_WAIT_EXCEEDED",
        message: "Cart lock wait exceeded safe threshold.",
        severity: "ERROR",
        occurrences: 16,
        firstSeen: "10:11",
        lastSeen: "10:26",
        trend: "Stable",
      },
    ],
    recentAlerts: [
      {
        id: "alert-payment-timeout",
        severity: "CRITICAL",
        ruleName: "Payment failures",
        logSamples: [{ level: "CRITICAL", message: "Payment gateway timeout crossed alert threshold." }],
        occurrences: 28,
        status: "OPEN",
        lastSeen: "10:29",
      },
      {
        id: "alert-cart-lock",
        severity: "ERROR",
        ruleName: "Checkout lock contention",
        logSamples: [{ level: "ERROR", message: "Cart lock wait exceeded the configured warning threshold." }],
        occurrences: 16,
        status: "ACKNOWLEDGED",
        lastSeen: "10:26",
      },
    ],
    problemLogs: [
      {
        id: "log-1",
        timestamp: "10:29:42",
        level: "CRITICAL",
        fingerprint: "PAYMENT_GATEWAY_TIMEOUT",
        message: "Payment gateway timeout after 3000ms for order 90321.",
        traceId: "trace-7f21c",
      },
      {
        id: "log-2",
        timestamp: "10:26:08",
        level: "ERROR",
        fingerprint: "CART_LOCK_WAIT_EXCEEDED",
        message: "Cart lock wait exceeded safe threshold for cart 8181.",
        traceId: "trace-21b90",
      },
    ],
  },
  "billing-worker": {
    application: applications[1],
    applications,
    generatedAt: "2026-06-23T10:30:00Z",
    comparisonSummary,
    hourlyErrorRates,
    stabilityRanking,
    healthScore: 71,
    healthStatus: "DEGRADED",
    healthReason:
      "Invoice retry storms are elevated but critical volume is contained.",
    metrics: [
      {
        id: "total-logs",
        label: "Total logs",
        value: "4,512",
        tone: "neutral",
      },
      { id: "error-rate", label: "Error rate", value: "4.2%", tone: "warning" },
      {
        id: "critical-logs",
        label: "Critical logs",
        value: "6",
        tone: "error",
      },
      { id: "open-alerts", label: "Open alerts", value: "2", tone: "warning" },
      {
        id: "fingerprints",
        label: "Fingerprints",
        value: "11",
        tone: "warning",
      },
      { id: "last-seen", label: "Last seen", value: "10:27", tone: "success" },
    ],
    trend: [
      { time: "10:15", totalLogs: 880, errorRate: 2.8, criticalAlerts: 0 },
      { time: "10:18", totalLogs: 910, errorRate: 3.1, criticalAlerts: 1 },
      { time: "10:21", totalLogs: 1020, errorRate: 3.7, criticalAlerts: 1 },
      { time: "10:24", totalLogs: 980, errorRate: 3.8, criticalAlerts: 1 },
      { time: "10:27", totalLogs: 1110, errorRate: 4.2, criticalAlerts: 2 },
    ],
    severityBreakdown: [
      { level: "INFO", count: 3900, percentage: 86 },
      { level: "WARN", count: 418, percentage: 9 },
      { level: "ERROR", count: 188, percentage: 4 },
      { level: "CRITICAL", count: 6, percentage: 1 },
    ],
    topFingerprints: [
      {
        id: "fp-invoice-retry",
        fingerprint: "INVOICE_RETRY_STORM",
        message: "Invoice retries are climbing faster than drain rate.",
        severity: "CRITICAL",
        occurrences: 19,
        firstSeen: "10:06",
        lastSeen: "10:27",
        trend: "Increasing",
      },
    ],
    recentAlerts: [
      {
        id: "alert-invoice-retry",
        severity: "CRITICAL",
        ruleName: "Invoice retry storm",
        logSamples: [{ level: "CRITICAL", message: "Invoice retries exceeded critical threshold." }],
        occurrences: 19,
        status: "OPEN",
        lastSeen: "10:27",
      },
    ],
    problemLogs: [
      {
        id: "log-billing-1",
        timestamp: "10:27:11",
        level: "CRITICAL",
        fingerprint: "INVOICE_RETRY_STORM",
        message: "Invoice retry queue exceeded worker drain rate.",
        traceId: "trace-b882",
      },
    ],
  },
  "identity-service": {
    application: applications[2],
    applications,
    generatedAt: "2026-06-23T10:30:00Z",
    comparisonSummary,
    hourlyErrorRates,
    stabilityRanking,
    healthScore: 86,
    healthStatus: "HEALTHY",
    healthReason:
      "Authentication errors are present but stable and alerts are under control.",
    metrics: [
      {
        id: "total-logs",
        label: "Total logs",
        value: "2,380",
        tone: "neutral",
      },
      { id: "error-rate", label: "Error rate", value: "3.1%", tone: "warning" },
      {
        id: "critical-logs",
        label: "Critical logs",
        value: "2",
        tone: "warning",
      },
      { id: "open-alerts", label: "Open alerts", value: "1", tone: "warning" },
      {
        id: "fingerprints",
        label: "Fingerprints",
        value: "7",
        tone: "neutral",
      },
      { id: "last-seen", label: "Last seen", value: "10:25", tone: "success" },
    ],
    trend: [
      { time: "10:15", totalLogs: 420, errorRate: 2.9, criticalAlerts: 0 },
      { time: "10:18", totalLogs: 450, errorRate: 3.0, criticalAlerts: 0 },
      { time: "10:21", totalLogs: 470, errorRate: 3.2, criticalAlerts: 1 },
      { time: "10:24", totalLogs: 510, errorRate: 3.1, criticalAlerts: 1 },
    ],
    severityBreakdown: [
      { level: "INFO", count: 2110, percentage: 89 },
      { level: "WARN", count: 194, percentage: 8 },
      { level: "ERROR", count: 74, percentage: 3 },
      { level: "CRITICAL", count: 2, percentage: 0 },
    ],
    topFingerprints: [
      {
        id: "fp-jwt-refresh",
        fingerprint: "JWT_REFRESH_REJECTED",
        message: "Refresh token rejection rate exceeded warning threshold.",
        severity: "ERROR",
        occurrences: 11,
        firstSeen: "10:12",
        lastSeen: "10:24",
        trend: "Stable",
      },
    ],
    recentAlerts: [
      {
        id: "alert-jwt-refresh",
        severity: "ERROR",
        ruleName: "Token refresh failures",
        logSamples: [{ level: "ERROR", message: "Refresh token rejection rate exceeded warning threshold." }],
        occurrences: 11,
        status: "ACKNOWLEDGED",
        lastSeen: "10:24",
      },
    ],
    problemLogs: [
      {
        id: "log-identity-1",
        timestamp: "10:24:17",
        level: "ERROR",
        fingerprint: "JWT_REFRESH_REJECTED",
        message: "Refresh token rejected for revoked session.",
        traceId: "trace-a118",
      },
    ],
  },
};

export async function getApplicationHealthSnapshot(
  applicationId = "checkout-api",
  window: HealthWindow = "15m",
): Promise<ApplicationHealthSnapshot> {
  const snapshot =
    baseSnapshots[applicationId] ?? baseSnapshots["checkout-api"];
  return Promise.resolve({
    ...snapshot,
    window: windowLabels[window],
  });
}
