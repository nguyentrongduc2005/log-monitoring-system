import { isAxiosError } from "axios";
import { apiClient } from "@/api/client";
import type {
  AlertRule,
  AlertRuleDraft,
  AlertRuleRequest,
  AlertRuleStatus,
  ApiEnvelope,
  ChatRoom
} from "./alert-rules-types";

function requireData<T>(envelope: ApiEnvelope<T>, fallbackMessage: string): T {
  if (envelope.data === undefined || envelope.data === null) {
    throw new Error(envelope.message || fallbackMessage);
  }
  return envelope.data;
}

export function getAlertingError(
  error: unknown,
  fallbackMessage = "Unable to complete this action."
) {
  if (!isAxiosError(error)) {
    return error instanceof Error ? error.message : fallbackMessage;
  }
  if (!error.response) {
    return "Unable to reach the server. Check your connection.";
  }
  const data = error.response.data;
  if (
    typeof data === "object" &&
    data !== null &&
    "message" in data &&
    typeof data.message === "string"
  ) {
    return data.message;
  }
  return fallbackMessage;
}

function toRequest(draft: AlertRuleDraft, includeApplication: boolean): AlertRuleRequest {
  const activeStartTime = draft.activeAllDay ? null : draft.activeStartTime;
  const activeEndTime = draft.activeAllDay ? null : draft.activeEndTime;
  const deliveryTargets = [
    ...(draft.websocketEnabled
      ? [{ channel: "WEBSOCKET" as const, chatRoomId: null }]
      : []),
    ...draft.telegramChatRoomIds.map(chatRoomId => ({
      channel: "TELEGRAM" as const,
      chatRoomId
    }))
  ];

  return {
    ...(includeApplication ? { applicationId: draft.applicationId } : {}),
    name: draft.name.trim(),
    description: draft.description.trim() || undefined,
    minSeverity: draft.minSeverity,
    severity: draft.severity,
    keywordPattern: draft.keywordPattern.trim() || undefined,
    thresholdCount: draft.thresholdCount,
    thresholdWindowSeconds: draft.thresholdWindowSeconds,
    cooldownSeconds: draft.cooldownSeconds,
    activeStartTime,
    activeEndTime,
    deliveryTargets
  };
}

export async function getAlertRules(applicationId?: string): Promise<AlertRule[]> {
  const response = await apiClient.get<ApiEnvelope<AlertRule[]>>("/alert-rules", {
    params: applicationId ? { applicationId } : undefined
  });
  return requireData(response.data, "Unable to load alert rules.");
}

export async function saveAlertRule(
  draft: AlertRuleDraft,
  id?: string
): Promise<AlertRule> {
  const response = id
    ? await apiClient.put<ApiEnvelope<AlertRule>>(
        `/alert-rules/${id}`,
        toRequest(draft, false)
      )
    : await apiClient.post<ApiEnvelope<AlertRule>>(
        "/alert-rules",
        toRequest(draft, true)
      );
  return requireData(response.data, "Unable to save alert rule.");
}

export async function changeAlertRuleStatus(
  id: string,
  status: AlertRuleStatus
): Promise<AlertRule> {
  const response = await apiClient.put<ApiEnvelope<AlertRule>>(
    `/alert-rules/${id}/status`,
    { status }
  );
  return requireData(response.data, "Unable to update alert rule status.");
}

export async function toggleAlertRule(rule: AlertRule): Promise<AlertRule> {
  return changeAlertRuleStatus(
    rule.id,
    rule.status === "ACTIVE" ? "DISABLED" : "ACTIVE"
  );
}

export async function deleteAlertRule(id: string): Promise<void> {
  await apiClient.delete<ApiEnvelope<void>>(`/alert-rules/${id}`);
}

export async function getTelegramChatRooms(status?: "ACTIVE" | "DISABLED"): Promise<ChatRoom[]> {
  const response = await apiClient.get<ApiEnvelope<ChatRoom[]>>(
    "/alert-chat-rooms",
    { params: { channel: "TELEGRAM", ...(status ? { status } : {}) } }
  );
  return requireData(response.data, "Unable to load Telegram chat rooms.");
}
