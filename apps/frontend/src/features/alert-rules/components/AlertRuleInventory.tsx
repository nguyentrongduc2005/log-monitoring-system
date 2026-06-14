import {
  managementButtonClass,
  managementInputClass,
  managementPanelClass,
  StatusBadge
} from "@/shared/components/management-ui";
import type { AlertRule, AlertRuleStatus } from "../alert-rules-types";
import {
  formatDate,
  formatRuleExpression,
  severityTone,
  statusTone
} from "./alert-rule-ui";

type AlertRuleInventoryProps = {
  error: string | null;
  filteredRules: AlertRule[];
  loading: boolean;
  saving: boolean;
  search: string;
  statusFilter: "ALL" | AlertRuleStatus;
  onEdit: (rule: AlertRule) => void;
  onRefresh: () => void;
  onSearchChange: (value: string) => void;
  onStatusFilterChange: (value: "ALL" | AlertRuleStatus) => void;
  onToggle: (rule: AlertRule) => void;
};

export default function AlertRuleInventory({
  error,
  filteredRules,
  loading,
  saving,
  search,
  statusFilter,
  onEdit,
  onRefresh,
  onSearchChange,
  onStatusFilterChange,
  onToggle
}: AlertRuleInventoryProps) {
  return (
    <section className={managementPanelClass}>
      <div className="flex flex-col gap-3 border-b border-border bg-surface-raised/35 p-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 className="text-base font-semibold text-text">
            Active Rule Inventory
          </h2>
          <p className="mt-1 text-sm text-muted">
            Review, edit, and mute alert rules owned by the alerting module.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <input
            aria-label="Search alert rules"
            className={managementInputClass}
            onChange={event => onSearchChange(event.target.value)}
            placeholder="Search rules, apps, or metrics..."
            value={search}
          />
          <select
            aria-label="Filter rule status"
            className={managementInputClass}
            onChange={event =>
              onStatusFilterChange(event.target.value as "ALL" | AlertRuleStatus)
            }
            value={statusFilter}
          >
            <option value="ALL">All rules</option>
            <option value="RUNNING">Running</option>
            <option value="MUTED">Muted</option>
          </select>
          <button className={managementButtonClass} onClick={onRefresh} type="button">
            Refresh
          </button>
        </div>
      </div>

      {loading ? <div className="p-4 text-sm text-muted">Loading alert rules...</div> : null}

      {error ? (
        <p className="m-4 rounded-md border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
          {error}
        </p>
      ) : null}

      {!loading ? (
        <div className="divide-y divide-border">
          {filteredRules.map(rule => (
            <article
              className={`grid gap-3 p-4 lg:grid-cols-[minmax(12rem,1fr)_minmax(20rem,1.3fr)_12rem_10rem] lg:items-center ${
                rule.status === "MUTED" ? "opacity-60" : ""
              }`}
              key={rule.id}
            >
              <div className="flex min-w-0 gap-3">
                <div
                  className={`grid size-10 shrink-0 place-items-center rounded-md border ${iconClass(rule.severity)}`}
                >
                  {rule.severity[0]}
                </div>
                <div className="min-w-0">
                  <p className="truncate font-semibold text-text">{rule.name}</p>
                  <p className="mt-1 text-xs uppercase text-muted">
                    {rule.serviceName} · {rule.severity}
                  </p>
                </div>
              </div>

              <div className="rounded-md border border-border bg-background px-3 py-2">
                <p className="font-mono text-xs font-semibold text-success">
                  RULE: {formatRuleExpression(rule)}
                </p>
              </div>

              <div className="text-xs text-muted">
                <StatusBadge tone={statusTone(rule.status)}>
                  {rule.status === "RUNNING" ? "Running" : "Muted"}
                </StatusBadge>
                <p className="mt-2">
                  Channel:{" "}
                  <span className="text-text">
                    {rule.channelType} {rule.channelTarget}
                  </span>
                </p>
                <p className="mt-1">Last: {formatDate(rule.lastTriggeredAt)}</p>
              </div>

              <div className="flex justify-end gap-2">
                <button
                  className={managementButtonClass}
                  onClick={() => onEdit(rule)}
                  type="button"
                >
                  Edit
                </button>
                <button
                  className="rounded-md bg-primary/15 px-3 py-2 text-xs font-medium text-primary transition hover:bg-primary/25 disabled:opacity-50"
                  disabled={saving}
                  onClick={() => onToggle(rule)}
                  type="button"
                >
                  {rule.status === "RUNNING" ? "Mute" : "Run"}
                </button>
              </div>
            </article>
          ))}

          {filteredRules.length === 0 ? (
            <div className="px-5 py-10 text-center">
              <p className="font-medium text-text">No alert rules found</p>
              <p className="mt-1 text-sm text-muted">
                Create a rule or adjust the filters.
              </p>
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

function iconClass(severity: AlertRule["severity"]) {
  const tone = severityTone(severity);
  if (tone === "error") {
    return "border-error/30 bg-error/15 text-error";
  }
  if (tone === "primary") {
    return "border-primary/30 bg-primary/15 text-primary";
  }
  return "border-warning/30 bg-warning/15 text-warning";
}
