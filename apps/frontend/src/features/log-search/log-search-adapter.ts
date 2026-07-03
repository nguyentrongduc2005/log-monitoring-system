import { apiClient } from "@/api/client";
import type {
  LogSearchFilters,
  LogSearchSnapshot,
} from "@/features/log-search/log-search-types";

interface RawLogVolumePoint {
  time: string;
  INFO?: number;
  WARN?: number;
  ERROR?: number;
  CRITICAL?: number;
  info?: number;
  warn?: number;
  error?: number;
  critical?: number;
}

interface RawLogSearchSnapshot extends Omit<LogSearchSnapshot, "buckets"> {
  buckets: RawLogVolumePoint[];
}

export async function searchLogs(
  filters: LogSearchFilters,
  selectedLogId?: string,
  page?: number,
  pageSize?: number,
): Promise<LogSearchSnapshot> {
  const response = await apiClient.get<RawLogSearchSnapshot>("/logs/search", {
    params: {
      query: filters.query || undefined,
      applicationId: filters.applicationId || undefined,
      level: filters.level || undefined,
      range: filters.range || undefined,
      page: page ?? 1,
      pageSize: pageSize ?? 20,
      selectedLogId: selectedLogId || undefined,
    },
  });

  const data = response.data;
  const buckets = (data.buckets || []).map((b) => {
    const info = Number(b.info ?? b.INFO ?? 0);
    const warn = Number(b.warn ?? b.WARN ?? 0);
    const error = Number(b.error ?? b.ERROR ?? 0);
    const critical = Number(b.critical ?? b.CRITICAL ?? 0);
    const total = info + warn + error + critical;
    return {
      time: b.time || "",
      info,
      warn,
      error,
      critical,
      total,
    };
  });

  return {
    ...data,
    buckets,
  };
}
