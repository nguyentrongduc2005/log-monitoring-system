import { createBrowserRouter } from "react-router-dom";
import AdminRoute from "@/shared/components/AdminRoute";
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
          },
          {
            path: "/profile",
            lazy: () => import("@/features/profile/ProfilePage")
          },
          {
            element: <AdminRoute />,
            children: [
              {
                path: "/admin/applications",
                lazy: () => import("@/features/applications/ApplicationsPage")
              },
              {
                path: "/admin/users",
                lazy: () => import("@/features/user-access/UserAccessPage")
              },
              {
                path: "/admin/alert-rules",
                lazy: () => import("@/features/alert-rules/AlertRulesPage")
              },
              {
                path: "/admin/retention",
                lazy: () => import("@/features/retention/RetentionPage")
              }
            ]
          }
        ]
      }
    ]
  }
]);
