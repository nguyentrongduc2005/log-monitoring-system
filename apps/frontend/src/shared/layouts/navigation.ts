import type { AppIconName } from "@/shared/components/AppIcon";

export type UserRole = "ADMIN" | "ENGINEER";

export type NavigationItemId =
  | "overview"
  | "live-logs"
  | "log-search"
  | "alerts"
  | "incidents"
  | "application-health"
  | "ai-insights"
  | "applications"
  | "users-access"
  | "alert-rules"
  | "notification-channels"
  | "retention-policies"
  | "system-operations"
  | "settings";

export type NavigationItem = {
  id: NavigationItemId;
  label: string;
  icon: AppIconName;
  to?: string;
};

export type NavigationGroup = {
  label: string;
  roles?: readonly UserRole[];
  items: readonly NavigationItem[];
};

export const navigationGroups = [
  {
    label: "Monitoring",
    items: [
      { id: "overview", label: "Overview", icon: "overview", to: "/" },
      {
        id: "live-logs",
        label: "Live Logs",
        icon: "live-logs",
        to: "/logs"
      },
      { id: "log-search", label: "Log Search", icon: "search" },
      { id: "alerts", label: "Alerts", icon: "alerts" },
      { id: "incidents", label: "Incidents", icon: "incidents" }
    ]
  },
  {
    label: "Analytics",
    items: [
      {
        id: "application-health",
        label: "Application Health",
        icon: "health"
      },
      { id: "ai-insights", label: "AI Insights", icon: "ai" }
    ]
  },
  {
    label: "Resources",
    items: [
      { id: "applications", label: "Applications", icon: "applications" }
    ]
  },
  {
    label: "Administration",
    roles: ["ADMIN"],
    items: [
      { id: "users-access", label: "Users & Access", icon: "users" },
      { id: "alert-rules", label: "Alert Rules", icon: "rules" },
      {
        id: "notification-channels",
        label: "Notification Channels",
        icon: "channels"
      },
      {
        id: "retention-policies",
        label: "Retention Policies",
        icon: "retention"
      },
      {
        id: "system-operations",
        label: "System Operations",
        icon: "operations"
      },
      { id: "settings", label: "Settings", icon: "settings" }
    ]
  }
] as const satisfies readonly NavigationGroup[];

export function getNavigationGroups(role?: UserRole) {
  return navigationGroups.filter(
    (group) => !("roles" in group) || group.roles.includes(role as "ADMIN")
  );
}

export function formatNavigationBadge(count?: number) {
  if (!count || count <= 0) {
    return null;
  }

  return count >= 100 ? "99+" : String(count);
}
