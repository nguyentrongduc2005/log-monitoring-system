import { createBrowserRouter } from "react-router-dom";

export const router = createBrowserRouter([
  {
    path: "/",
    lazy: () => import("@/features/dashboard/DashboardPage")
  },
  {
    path: "/logs",
    lazy: () => import("@/features/live-logs/LiveLogsPage")
  },
//   {
//     path: "/applications",
//     lazy: () => import("@/features/applications/ApplicationsPage")
//   },
//   {
//     path: "/health",
//     lazy: () => import("@/features/application-health/ApplicationHealthPage")
//   },
//   {
//     path: "/alerts",
//     lazy: () => import("@/features/alerts/AlertsPage")
//   }
]);