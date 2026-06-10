import type {
  LiveLogEntry,
  LiveLogFilters,
  LiveLogSnapshot
} from "@/features/live-logs/live-logs-types";

export const MAX_VISIBLE_LOGS = 300;

const snapshot: LiveLogSnapshot = {
  applications: [
    { id: "checkout-api", name: "checkout-api" },
    { id: "billing-worker", name: "billing-worker" },
    { id: "identity-service", name: "identity-service" }
  ],
  entries: [
    {
      id: "log-1",
      timestamp: "2026-06-09T10:15:04Z",
      applicationId: "checkout-api",
      applicationName: "checkout-api",
      level: "CRITICAL",
      message: "Payment gateway timeout exceeded the critical threshold.",
      traceId: "trace-checkout-1",
      eventId: "evt-1001",
      ingestionId: "ing-9001",
      source: "api",
      host: "checkout-01",
      environment: "production",
      attributes: {
        region: "ap-southeast-1",
        fingerprint: "PAYMENT_GATEWAY_TIMEOUT"
      }
    },
    {
      id: "log-2",
      timestamp: "2026-06-09T10:15:01Z",
      applicationId: "billing-worker",
      applicationName: "billing-worker",
      level: "ERROR",
      message: "Invoice retry queue is growing faster than the worker drain rate.",
      traceId: "trace-billing-1",
      eventId: "evt-1002",
      ingestionId: "ing-9002",
      source: "worker",
      host: "billing-02",
      environment: "production",
      attributes: {
        queue: "invoice-retries",
        shard: "b-02"
      }
    },
    {
      id: "log-3",
      timestamp: "2026-06-09T10:14:57Z",
      applicationId: "identity-service",
      applicationName: "identity-service",
      level: "WARN",
      message: "Refresh token latency exceeded the staging baseline.",
      traceId: "trace-identity-7",
      eventId: "evt-1003",
      ingestionId: "ing-9003",
      source: "api",
      host: "identity-03",
      environment: "staging"
    },
    {
      id: "log-4",
      timestamp: "2026-06-09T10:14:50Z",
      applicationId: "checkout-api",
      applicationName: "checkout-api",
      level: "INFO",
      message: "Order checkout completed successfully.",
      traceId: "trace-checkout-2",
      eventId: "evt-1004",
      ingestionId: "ing-9004",
      source: "api",
      host: "checkout-01",
      environment: "production"
    }
  ],
  connectionState: "live",
  buffered: 4,
  dropped: 0
};

export async function getInitialLiveLogSnapshot(): Promise<LiveLogSnapshot> {
  return Promise.resolve(snapshot);
}

export function filterLiveLogEntries(
  entries: LiveLogEntry[],
  filters: LiveLogFilters
): LiveLogEntry[] {
  const keyword = filters.keyword.trim().toLowerCase();
  const traceId = filters.traceId.trim().toLowerCase();

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

    if (traceId && !entry.traceId?.toLowerCase().includes(traceId)) {
      return false;
    }

    return true;
  });
}
