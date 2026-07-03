import { Client, type StompSubscription } from "@stomp/stompjs";
import { apiClient } from "@/api/client";
import type {
  ApiEnvelope,
  Application
} from "@/features/applications/application-types";
import type {
  ApplicationOption,
  LiveConnectionState,
  LiveLogEntry,
  LiveLogFilters,
  LiveLogSnapshot,
  LogLevel
} from "@/features/live-logs/live-logs-types";

export const MAX_VISIBLE_LOGS = 300;

type LiveLogMessage = {
  eventId: string;
  ingestionId: string;
  applicationId: string;
  applicationName: string;
  applicationDisplayName?: string | null;
  level: LogLevel | string;
  message: string;
  traceId?: string | null;
  logTimestamp: string;
  processedAt: string;
};

type LiveLogConnectionOptions = {
  accessToken: string;
  applications: ApplicationOption[];
  onLog: (entry: LiveLogEntry) => void;
  onStateChange: (state: LiveConnectionState) => void;
};

type LiveLogConnection = {
  disconnect: () => void;
};

function requireData<T>(
  envelope: ApiEnvelope<T>,
  fallbackMessage: string
): T {
  if (envelope.data === undefined || envelope.data === null) {
    throw new Error(envelope.message || fallbackMessage);
  }

  return envelope.data;
}

export async function getInitialLiveLogSnapshot(): Promise<LiveLogSnapshot> {
  const response =
    await apiClient.get<ApiEnvelope<Application[]>>("/applications/me");
  const applications = requireData(
    response.data,
    "Unable to load authorized applications."
  )
    .map(toApplicationOption)
    .filter((application) => application.id !== "");

  return {
    applications,
    entries: [],
    connectionState: applications.length > 0 ? "connecting" : "disconnected",
    buffered: 0,
    dropped: 0
  };
}

export function createLiveLogConnection({
  accessToken,
  applications,
  onLog,
  onStateChange
}: LiveLogConnectionOptions): LiveLogConnection {
  const subscriptions: StompSubscription[] = [];
  const client = new Client({
    brokerURL: normalizeWebSocketUrl(import.meta.env.VITE_WS_URL),
    connectHeaders: {
      Authorization: `Bearer ${accessToken}`
    },
    reconnectDelay: 5_000,
    onConnect: () => {
      onStateChange("live");
      applications.forEach((application) => {
        subscriptions.push(
          client.subscribe(
            `/topic/applications/${application.id}/logs`,
            (message) => {
              onLog(toLiveLogEntry(JSON.parse(message.body) as LiveLogMessage));
            }
          )
        );
      });
    },
    onWebSocketClose: () => {
      onStateChange("disconnected");
    },
    onStompError: () => {
      onStateChange("error");
    },
    onWebSocketError: () => {
      onStateChange("error");
    }
  });

  onStateChange("connecting");
  client.activate();

  return {
    disconnect: () => {
      subscriptions
        .splice(0)
        .forEach((subscription) => subscription.unsubscribe());
      void client.deactivate();
    }
  };
}

export function filterLiveLogEntries(
  entries: LiveLogEntry[],
  filters: LiveLogFilters
): LiveLogEntry[] {
  const keyword = filters.keyword.trim().toLowerCase();

  return entries.filter((entry) => {
    if (filters.applicationId && entry.applicationId !== filters.applicationId) {
      return false;
    }

    if (filters.level !== "ALL" && entry.level !== filters.level) {
      return false;
    }

    if (keyword && !entry.message.toLowerCase().includes(keyword)) {
      return false;
    }

    return true;
  });
}

function toApplicationOption(application: Application): ApplicationOption {
  return {
    id: application.id ?? "",
    name: application.displayName || application.name || "Unnamed application"
  };
}

function toLiveLogEntry(message: LiveLogMessage): LiveLogEntry {
  return {
    id: message.eventId,
    timestamp: message.logTimestamp,
    applicationId: message.applicationId,
    applicationName: message.applicationDisplayName || message.applicationName,
    level: normalizeLevel(message.level),
    message: message.message,
    traceId: message.traceId ?? undefined,
    eventId: message.eventId,
    ingestionId: message.ingestionId
  };
}

function normalizeLevel(level: string): LogLevel {
  if (
    level === "INFO" ||
    level === "WARN" ||
    level === "ERROR" ||
    level === "CRITICAL"
  ) {
    return level;
  }

  return "INFO";
}

function normalizeWebSocketUrl(value: string): string {
  if (value.startsWith("http://")) {
    return value.replace("http://", "ws://");
  }

  if (value.startsWith("https://")) {
    return value.replace("https://", "wss://");
  }

  return value;
}
