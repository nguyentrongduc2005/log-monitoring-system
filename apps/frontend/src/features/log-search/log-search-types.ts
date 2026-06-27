export type LogSearchLevel = "INFO" | "WARN" | "ERROR" | "CRITICAL";

export type LogSearchRange = "15m" | "1h" | "6h" | "24h";

export type LogSearchApplication = {
  id: string;
  name: string;
};

export type LogSearchEntry = {
  id: string;
  timestamp: string;
  applicationId: string;
  applicationName: string;
  level: LogSearchLevel;
  message: string;
  traceId: string;
  spanId: string;
  eventId: string;
  source: string;
  host: string;
  durationMs?: number;
  statusCode?: number;
  attributes: Record<string, string>;
  stack?: string[];
};

export type LogSearchBucket = {
  time: string;
  total: number;
  info: number;
  warn: number;
  error: number;
  critical: number;
};

export type LogSearchSummary = {
  totalMatches: number;
  errorMatches: number;
  criticalMatches: number;
  uniqueTraces: number;
  slowestDurationMs: number;
};

export type LogSearchFilters = {
  query: string;
  applicationId: string;
  level: "ALL" | LogSearchLevel;
  range: LogSearchRange;
};

export type LogSearchSnapshot = {
  applications: LogSearchApplication[];
  summary: LogSearchSummary;
  buckets: LogSearchBucket[];
  results: LogSearchEntry[];
  relatedTrace: LogSearchEntry[];
};
