import { apiClient } from "@/api/client";
import type {
  LogSearchFilters,
  LogSearchSnapshot,
} from "@/features/log-search/log-search-types";

export async function searchLogs(
  filters: LogSearchFilters,
  selectedLogId?: string,
  page?: number,
  pageSize?: number,
): Promise<LogSearchSnapshot> {
  const response = await apiClient.get<LogSearchSnapshot>("/api/v1/logs/search", {
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
  return response.data;
}
