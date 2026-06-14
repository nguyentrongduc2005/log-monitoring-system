import type {
  AlertMetric,
  AlertRule,
  AlertRuleStatus,
  AlertSeverity
} from "../alert-rules-types";

export function formatDate(value?: string) {
  if (!value) {
    return "Never";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Unknown" : date.toLocaleString();
}

export function severityTone(severity: AlertSeverity) {
  if (severity === "CRITICAL") {
    return "error";
  }
  if (severity === "ERROR") {
    return "primary";
  }
  return "warning";
}

export function statusTone(status: AlertRuleStatus) {
  return status === "RUNNING" ? "success" : "muted";
}

export function metricLabel(metric: AlertMetric) {
  if (metric === "LOG_COUNT") {
    return "logs.filter(status == 401).count()";
  }
  if (metric === "LATENCY_P95") {
    return "metrics.latency.p95";
  }
  return "metrics.disk_usage";
}

export function formatWindow(seconds: number) {
  if (seconds < 60) {
    return `${seconds}s`;
  }
  return `${seconds / 60}min`;
}

export function formatRuleExpression(rule: AlertRule) {
  return `${metricLabel(rule.metric)} ${rule.operator} ${rule.threshold} per ${formatWindow(rule.windowSeconds)}`;
}
