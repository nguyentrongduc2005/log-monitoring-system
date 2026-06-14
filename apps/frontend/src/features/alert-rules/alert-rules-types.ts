export type AlertRuleStatus = "RUNNING" | "MUTED";

export type AlertSeverity = "CRITICAL" | "ERROR" | "WARN";

export type AlertMetric = "LOG_COUNT" | "LATENCY_P95" | "DISK_USAGE";

export type AlertOperator = ">" | ">=" | "<";

export type AlertChannelType = "Telegram" | "Email" | "Webhook";

export type AlertRule = {
  id: string;
  name: string;
  applicationName: string;
  serviceName: string;
  severity: AlertSeverity;
  metric: AlertMetric;
  operator: AlertOperator;
  threshold: number;
  windowSeconds: number;
  channelType: AlertChannelType;
  channelTarget: string;
  status: AlertRuleStatus;
  triggered24h: number;
  breached24h: number;
  lastTriggeredAt?: string;
};

export type AlertRuleDraft = Pick<
  AlertRule,
  | "name"
  | "applicationName"
  | "serviceName"
  | "severity"
  | "metric"
  | "operator"
  | "threshold"
  | "windowSeconds"
  | "channelType"
  | "channelTarget"
>;
