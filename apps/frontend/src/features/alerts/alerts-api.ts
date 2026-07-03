import { Client, type StompSubscription } from "@stomp/stompjs";
import { apiClient } from "@/api/client";
import type { ApiEnvelope, Application } from "@/features/applications/application-types";
import type { Alert, AlertApplication, AlertConnectionState, AlertSeverity } from "./alerts-types";

type AlertNotificationMessage = Omit<Alert, "id" | "status"> & { alertId: string };

function requireData<T>(envelope: ApiEnvelope<T>, message: string): T {
  if (envelope.data === undefined || envelope.data === null) throw new Error(envelope.message || message);
  return envelope.data;
}

export async function getAlertCenterSnapshot() {
  const [applicationsResponse, alertsResponse] = await Promise.all([
    apiClient.get<ApiEnvelope<Application[]>>("/applications/me"),
    apiClient.get<ApiEnvelope<Alert[]>>("/alerts")
  ]);
  const applications = requireData(applicationsResponse.data, "Unable to load applications.")
    .flatMap<AlertApplication>(application => application.id ? [{ id: application.id, name: application.displayName || application.name || "Unnamed application" }] : []);
  return { applications, alerts: requireData(alertsResponse.data, "Unable to load alerts.") };
}

export async function acknowledgeAlert(id: string): Promise<Alert> {
  const response = await apiClient.put<ApiEnvelope<Alert>>(`/alerts/${id}/acknowledge`);
  return requireData(response.data, "Unable to acknowledge alert.");
}

export async function resolveAlert(id: string): Promise<Alert> {
  const response = await apiClient.put<ApiEnvelope<Alert>>(`/alerts/${id}/resolve`);
  return requireData(response.data, "Unable to resolve alert.");
}

export function createAlertConnection(options: {
  accessToken: string;
  applications: AlertApplication[];
  onAlert: (alert: Alert) => void;
  onStateChange: (state: AlertConnectionState) => void;
}) {
  const subscriptions: StompSubscription[] = [];
  const client = new Client({
    brokerURL: normalizeWebSocketUrl(import.meta.env.VITE_WS_URL),
    connectHeaders: { Authorization: `Bearer ${options.accessToken}` },
    reconnectDelay: 5_000,
    onConnect: () => {
      options.onStateChange("live");
      options.applications.forEach(application => subscriptions.push(client.subscribe(
        `/topic/applications/${application.id}/alerts`,
        message => options.onAlert(toAlert(JSON.parse(message.body) as AlertNotificationMessage))
      )));
    },
    onWebSocketClose: () => options.onStateChange("disconnected"),
    onWebSocketError: () => options.onStateChange("error"),
    onStompError: () => options.onStateChange("error")
  });
  options.onStateChange("connecting");
  client.activate();
  return () => {
    subscriptions.splice(0).forEach(subscription => subscription.unsubscribe());
    void client.deactivate();
  };
}

function toAlert(message: AlertNotificationMessage): Alert {
  return {
    ...message,
    id: message.alertId,
    severity: normalizeSeverity(message.severity),
    status: "OPEN"
  };
}

function normalizeSeverity(value: string): AlertSeverity {
  return value === "INFO" || value === "WARN" || value === "ERROR" || value === "CRITICAL" ? value : "ERROR";
}

function normalizeWebSocketUrl(value: string) {
  if (value.startsWith("http://")) return value.replace("http://", "ws://");
  if (value.startsWith("https://")) return value.replace("https://", "wss://");
  return value;
}
