export type LogLevel = "INFO" | "WARN" | "ERROR" | "CRITICAL";

export type LiveConnectionState =
  | "live"
  | "paused"
  | "connecting"
  | "reconnecting"
  | "disconnected"
  | "error";

export type ApplicationOption = {
  id: string;
  name: string;
};

export type LiveLogEntry = {
  id: string;
  timestamp: string;
  applicationId: string;
  applicationName: string;
  level: LogLevel;
  message: string;
  traceId?: string;
  eventId?: string;
  ingestionId?: string;
  source?: string;
  host?: string;
  environment?: string;
  attributes?: Record<string, string>;
};

export type LiveLogFilters = {
  applicationId: string;
  level: "ALL" | LogLevel;
  keyword: string;
  traceId: string;
};

export type LiveLogSnapshot = {
  applications: ApplicationOption[];
  entries: LiveLogEntry[];
  connectionState: LiveConnectionState;
  buffered: number;
  dropped: number;
};
