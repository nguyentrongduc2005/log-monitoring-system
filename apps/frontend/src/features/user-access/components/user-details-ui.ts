export function formatDate(value?: string) {
  if (!value) {
    return "Not available";
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Not available" : date.toLocaleString();
}

export function statusClasses(status?: string) {
  if (status === "ACTIVE") {
    return "bg-success/15 text-success";
  }
  if (status === "LOCKED") {
    return "bg-warning/15 text-warning";
  }
  return "bg-error/15 text-error";
}

export function initials(value?: string) {
  return (value || "U")
    .split(/\s+/)
    .slice(0, 2)
    .map(part => part[0])
    .join("")
    .toUpperCase();
}

