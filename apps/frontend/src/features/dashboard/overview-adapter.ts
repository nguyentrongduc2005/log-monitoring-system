import type {
  DashboardWindow,
  OverviewSnapshot,
} from "@/features/dashboard/overview-types";

import { apiClient } from "@/api/client";

export async function getOverviewSnapshot(
  window: DashboardWindow = "24h",
): Promise<OverviewSnapshot> {
  const response = await apiClient.get<OverviewSnapshot>(`/dashboard/overview?window=${window}`);
  return response.data;
}
