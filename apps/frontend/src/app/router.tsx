import { createBrowserRouter } from "react-router-dom";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import AppLayout from "@/shared/layouts/AppLayout";

export const router = createBrowserRouter([
  {
    path: "/login",
    lazy: () => import("@/features/auth/LoginPage")
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            path: "/",
            lazy: () => import("@/features/dashboard/DashboardPage")
          },
          {
            path: "/logs",
            lazy: () => import("@/features/live-logs/LiveLogsPage")
          }
        ]
      }
    ]
  }
]);
