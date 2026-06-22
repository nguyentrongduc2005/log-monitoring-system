export type AlertSeverity = "INFO" | "WARN" | "ERROR" | "CRITICAL";
export type AlertStatus = "OPEN" | "ACKNOWLEDGED" | "RESOLVED";

export type AlertApplication = { id: string; name: string };

export type Alert = {
  id: string;
  ruleId: string;
  applicationId: string;
  eventId?: string;
  ingestionId?: string;
  applicationName: string;
  applicationDisplayName?: string | null;
  severity: AlertSeverity;
  message: string;
  fingerprint: string;
  logTimestamp: string;
  triggeredAt: string;
  status: AlertStatus;
  dispatchedChannels?: string[];
  acknowledgedBy?: string | null;
  acknowledgedAt?: string | null;
  resolvedBy?: string | null;
  resolvedAt?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type AlertConnectionState = "connecting" | "live" | "disconnected" | "error";
