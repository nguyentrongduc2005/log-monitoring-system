import type {
  RetentionAction,
  RetentionLogLevel,
  RetentionOperationStatus
} from "../retention-types";

export function formatDate(value?: string) {
  if (!value) {
    return "Never";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Unknown" : date.toLocaleString();
}

export function logLevelTone(level: RetentionLogLevel) {
  if (level === "ERROR" || level === "CRITICAL") {
    return "error";
  }
  if (level === "WARN") {
    return "warning";
  }
  return "primary";
}

export function operationTone(status: RetentionOperationStatus) {
  return status === "SUCCESS" ? "success" : "warning";
}

export function actionLabel(action: RetentionAction) {
  return action[0] + action.slice(1).toLowerCase();
}
