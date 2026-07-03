export type AlertRuleStatus = "ACTIVE" | "DISABLED";

export type AlertSeverity = "INFO" | "WARN" | "ERROR" | "CRITICAL";

export type AlertChannel = "TELEGRAM" | "WEBSOCKET";

export type AlertDeliveryTarget = {
  channel: AlertChannel;
  chatRoomId?: string | null;
};

export type AlertRule = {
  id: string;
  applicationId: string;
  name: string;
  description?: string | null;
  minSeverity: AlertSeverity;
  severity: AlertSeverity;
  keywordPattern?: string | null;
  thresholdCount: number;
  thresholdWindowSeconds: number;
  cooldownSeconds: number;
  activeStartTime?: string | null;
  activeEndTime?: string | null;
  status: AlertRuleStatus;
  channels: AlertChannel[];
  deliveryTargets: AlertDeliveryTarget[];
  createdBy: string;
  createdAt: string;
  updatedAt: string;
};

export type AlertRuleDraft = {
  applicationId: string;
  name: string;
  description: string;
  minSeverity: AlertSeverity;
  severity: AlertSeverity;
  keywordPattern: string;
  thresholdCount: number;
  thresholdWindowSeconds: number;
  cooldownSeconds: number;
  activeAllDay: boolean;
  activeStartTime: string;
  activeEndTime: string;
  websocketEnabled: boolean;
  telegramChatRoomIds: string[];
};

export type AlertRuleRequest = {
  applicationId?: string;
  name: string;
  description?: string;
  minSeverity: AlertSeverity;
  severity: AlertSeverity;
  keywordPattern?: string;
  thresholdCount: number;
  thresholdWindowSeconds: number;
  cooldownSeconds: number;
  activeStartTime?: string | null;
  activeEndTime?: string | null;
  deliveryTargets: AlertDeliveryTarget[];
};

export type ChatRoomStatus = "ACTIVE" | "DISABLED";

export type ChatRoom = {
  id: string;
  channel: AlertChannel;
  name: string;
  chatId: string;
  description?: string | null;
  status: ChatRoomStatus;
  createdBy?: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ApiEnvelope<T> = {
  success?: boolean;
  message?: string;
  data?: T;
  timestamp?: string;
};
