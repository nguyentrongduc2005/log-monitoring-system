import type { AlertRule, AlertRuleDraft } from "./alert-rules-types";

let alertRules: AlertRule[] = [
  {
    id: "rule-auth-401-flood",
    name: "Auth-401-Flood",
    applicationName: "Payment Gateway",
    serviceName: "auth-service",
    severity: "CRITICAL",
    metric: "LOG_COUNT",
    operator: ">",
    threshold: 500,
    windowSeconds: 30,
    channelType: "Telegram",
    channelTarget: "#ops-critical",
    status: "RUNNING",
    triggered24h: 14,
    breached24h: 44,
    lastTriggeredAt: "2026-06-11T04:40:00Z"
  },
  {
    id: "rule-payment-latency",
    name: "Payment-Latency-Spike",
    applicationName: "Payment Gateway",
    serviceName: "payment-gateway",
    severity: "ERROR",
    metric: "LATENCY_P95",
    operator: ">",
    threshold: 2500,
    windowSeconds: 300,
    channelType: "Email",
    channelTarget: "payments-oncall@logpulse.local",
    status: "RUNNING",
    triggered24h: 8,
    breached24h: 31,
    lastTriggeredAt: "2026-06-11T03:20:00Z"
  },
  {
    id: "rule-disk-space",
    name: "Disk-Space-Warn",
    applicationName: "Database Node",
    serviceName: "clickhouse-cluster",
    severity: "WARN",
    metric: "DISK_USAGE",
    operator: ">",
    threshold: 85,
    windowSeconds: 600,
    channelType: "Webhook",
    channelTarget: "https://hooks.logpulse.local/storage",
    status: "MUTED",
    triggered24h: 0,
    breached24h: 3
  }
];

function delay() {
  return new Promise(resolve => window.setTimeout(resolve, 120));
}

export async function getAlertRules() {
  await delay();
  return alertRules.map(rule => ({ ...rule }));
}

export async function saveAlertRule(draft: AlertRuleDraft, id?: string) {
  await delay();

  if (id) {
    const current = alertRules.find(rule => rule.id === id);
    const updated: AlertRule = {
      ...draft,
      id,
      status: current?.status ?? "RUNNING",
      triggered24h: current?.triggered24h ?? 0,
      breached24h: current?.breached24h ?? 0,
      lastTriggeredAt: current?.lastTriggeredAt
    };
    alertRules = alertRules.map(rule => (rule.id === id ? updated : rule));
    return { ...updated };
  }

  const created: AlertRule = {
    ...draft,
    id: `rule-${Date.now()}`,
    status: "RUNNING",
    triggered24h: 0,
    breached24h: 0
  };
  alertRules = [created, ...alertRules];
  return { ...created };
}

export async function toggleAlertRule(id: string) {
  await delay();
  let updated: AlertRule | undefined;
  alertRules = alertRules.map(rule => {
    if (rule.id !== id) {
      return rule;
    }
    updated = {
      ...rule,
      status: rule.status === "RUNNING" ? "MUTED" : "RUNNING"
    };
    return updated;
  });

  if (!updated) {
    throw new Error("Alert rule not found");
  }

  return { ...updated };
}
