export function formatDate(value?: string) {
  if (!value) {
    return "Never";
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Unknown" : date.toLocaleString();
}

export function statusTone(status?: string) {
  if (status === "ACTIVE") {
    return "success";
  }
  if (status === "REVOKED" || status === "INACTIVE") {
    return "error";
  }
  return "warning";
}

