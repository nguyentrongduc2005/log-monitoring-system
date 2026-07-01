import {
  managementButtonClass,
  managementInputClass,
  managementPanelClass,
  StatusBadge
} from "@/shared/components/management-ui";
import type { AlertRule, AlertRuleStatus, ChatRoom } from "../alert-rules-types";

type AlertRuleInventoryProps = {
  applicationNames: Map<string, string>;
  chatRooms: ChatRoom[];
  error: string | null;
  filteredRules: AlertRule[];
  loading: boolean;
  saving: boolean;
  search: string;
  statusFilter: "ALL" | AlertRuleStatus;
  onDelete: (rule: AlertRule) => void;
  onEdit: (rule: AlertRule) => void;
  onRefresh: () => void;
  onSearchChange: (value: string) => void;
  onStatusFilterChange: (value: "ALL" | AlertRuleStatus) => void;
  onToggle: (rule: AlertRule) => void;
};

export default function AlertRuleInventory({
  applicationNames,
  chatRooms,
  error,
  filteredRules,
  loading,
  saving,
  search,
  statusFilter,
  onDelete,
  onEdit,
  onRefresh,
  onSearchChange,
  onStatusFilterChange,
  onToggle
}: AlertRuleInventoryProps) {
  const roomNames = new Map(chatRooms.map(room => [room.id, room.name]));

  return (
    <section className={managementPanelClass}>
      <div className="flex flex-col gap-3 border-b border-border bg-surface-raised/35 p-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h2 className="text-base font-semibold text-text">Rule inventory</h2>
          <p className="mt-1 text-sm text-muted">
            Review, edit, disable, or remove alert rules.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <input
            aria-label="Search alert rules"
            className={managementInputClass}
            onChange={event => onSearchChange(event.target.value)}
            placeholder="Search rules or applications..."
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
            <option value="ACTIVE">Active</option>
            <option value="DISABLED">Disabled</option>
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
          {filteredRules.map(rule => {
            const telegramRooms = rule.deliveryTargets
              .filter(target => target.channel === "TELEGRAM" && target.chatRoomId)
              .map(target => roomNames.get(target.chatRoomId!) || "Unknown room");
            const websocket = rule.deliveryTargets.some(
              target => target.channel === "WEBSOCKET"
            );

            return (
              <article
                className={`grid gap-4 p-4 xl:grid-cols-[minmax(14rem,1fr)_minmax(18rem,1.2fr)_11rem_auto] xl:items-center ${
                  rule.status === "DISABLED" ? "opacity-60" : ""
                }`}
                key={rule.id}
              >
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <p className="truncate font-semibold text-text">{rule.name}</p>
                    <StatusBadge tone={rule.status === "ACTIVE" ? "success" : "muted"}>
                      {rule.status}
                    </StatusBadge>
                  </div>
                  <p className="mt-1 text-xs text-muted">
                    {applicationNames.get(rule.applicationId) || rule.applicationId}
                  </p>
                  <p className="mt-2 text-xs text-muted">
                    Min severity: <span className="font-semibold text-text">{rule.minSeverity}</span>
                    <span className="mx-2 opacity-50">|</span>
                    Alert: <span className="font-semibold text-text">{rule.severity}</span>
                    {rule.keywordPattern ? ` · contains “${rule.keywordPattern}”` : ""}
                  </p>
                </div>

                <div className="rounded-md border border-border bg-background px-3 py-2 text-xs">
                  <p className="font-mono font-semibold text-primary">
                    {rule.thresholdCount} events / {rule.thresholdWindowSeconds}s
                  </p>
                  <p className="mt-1 text-muted">Cooldown: {rule.cooldownSeconds}s</p>
                  <p className="mt-1 text-muted">
                    Active: {formatActiveWindow(rule)}
                  </p>
                  <p className="mt-2 text-muted">
                    {[
                      ...(websocket ? ["WebSocket"] : []),
                      ...telegramRooms.map(room => `Telegram: ${room}`)
                    ].join(" · ")}
                  </p>
                </div>

                <p className="text-xs text-muted">
                  Updated<br />
                  <span className="text-text">{formatDate(rule.updatedAt)}</span>
                </p>

                <div className="flex flex-wrap justify-end gap-2">
                  <button className={managementButtonClass} onClick={() => onEdit(rule)} type="button">
                    Edit
                  </button>
                  <button
                    className={managementButtonClass}
                    disabled={saving}
                    onClick={() => onToggle(rule)}
                    type="button"
                  >
                    {rule.status === "ACTIVE" ? "Disable" : "Enable"}
                  </button>
                  <button
                    className="inline-flex min-h-9 items-center justify-center rounded-md border border-error/30 px-3 text-sm font-medium text-error transition hover:bg-error/10 disabled:opacity-50"
                    disabled={saving}
                    onClick={() => onDelete(rule)}
                    type="button"
                  >
                    Delete
                  </button>
                </div>
              </article>
            );
          })}

          {filteredRules.length === 0 ? (
            <div className="px-5 py-10 text-center">
              <p className="font-medium text-text">No alert rules found</p>
              <p className="mt-1 text-sm text-muted">Create a rule or adjust the filters.</p>
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

function formatActiveWindow(rule: AlertRule) {
  if (!rule.activeStartTime || !rule.activeEndTime) {
    return "All day";
  }
  return `${rule.activeStartTime}-${rule.activeEndTime}`;
}
