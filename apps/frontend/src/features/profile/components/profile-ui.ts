export function formatDate(value?: string) {
  if (!value) {
    return "Not available";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Not available";
  }

  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

export function getRoleDescription(role?: string) {
  if (role === "ADMIN") {
    return "Admins can manage users, roles, and system-wide operational settings.";
  }

  if (role === "ENGINEER") {
    return "Engineers can monitor the applications assigned to them and manage their own account.";
  }

  return "Role details are not available.";
}

export function getStatusDescription(status?: string) {
  if (status === "ACTIVE") {
    return "Your account is active and can access the platform.";
  }

  if (status === "DISABLED") {
    return "Your account is disabled. Contact an administrator if this is unexpected.";
  }

  if (status === "LOCKED") {
    return "Your account is locked. Contact an administrator to unlock it.";
  }

  return "Status details are not available.";
}

export function statusTone(status?: string) {
  if (status === "ACTIVE") {
    return "success";
  }
  if (status === "LOCKED") {
    return "warning";
  }
  if (status === "DISABLED") {
    return "error";
  }
  return "muted";
}

