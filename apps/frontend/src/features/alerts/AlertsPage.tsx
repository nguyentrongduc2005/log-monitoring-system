import { useMemo, useState } from "react";
import { managementButtonClass, managementInputClass, managementPanelClass, MetricCard, StatusBadge } from "@/shared/components/management-ui";
import { PageHeader } from "@/shared/layouts/page-header-context";
import { useAlertCenter } from "./alert-center-context";
import type { Alert, AlertSeverity, AlertStatus } from "./alerts-types";

export function Component() {
  const center = useAlertCenter();
  const [applicationId, setApplicationId] = useState("");
  const [status, setStatus] = useState<"ALL" | AlertStatus>("ALL");
  const [severity, setSeverity] = useState<"ALL" | AlertSeverity>("ALL");
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<Alert | null>(null);
  const filtered = useMemo(() => center.alerts.filter(alert =>
    (!applicationId || alert.applicationId === applicationId) &&
    (status === "ALL" || alert.status === status) &&
    (severity === "ALL" || alert.severity === severity) &&
    (!search.trim() || `${alert.message} ${alert.applicationName} ${alert.fingerprint}`.toLowerCase().includes(search.trim().toLowerCase()))
  ), [applicationId, center.alerts, search, severity, status]);

  return <div className="space-y-5">
    <PageHeader actions={<button className={managementButtonClass} onClick={() => void center.refresh()} type="button">Refresh</button>} title="Alerts" />
    <div>
      <h1 className="text-2xl font-semibold text-text">Alerts</h1>
      <p className="mt-1 text-sm text-muted">Alerts across applications you are authorized to view. Realtime: {center.connectionState}.</p>
    </div>
    <section className="grid gap-3 md:grid-cols-3">
      <MetricCard label="Open" tone="error" value={center.openCount} />
      <MetricCard label="Acknowledged" tone="warning" value={center.alerts.filter(item => item.status === "ACKNOWLEDGED").length} />
      <MetricCard label="Resolved" tone="success" value={center.alerts.filter(item => item.status === "RESOLVED").length} />
    </section>
    <section className={managementPanelClass}>
      <div className="grid gap-3 border-b border-border bg-surface-raised/35 p-4 md:grid-cols-4">
        <select aria-label="Filter application" className={managementInputClass} onChange={event => setApplicationId(event.target.value)} value={applicationId}>
          <option value="">All applications</option>{center.applications.map(app => <option key={app.id} value={app.id}>{app.name}</option>)}
        </select>
        <select aria-label="Filter alert status" className={managementInputClass} onChange={event => setStatus(event.target.value as "ALL" | AlertStatus)} value={status}>
          <option value="ALL">All statuses</option><option value="OPEN">Open</option><option value="ACKNOWLEDGED">Acknowledged</option><option value="RESOLVED">Resolved</option>
        </select>
        <select aria-label="Filter alert severity" className={managementInputClass} onChange={event => setSeverity(event.target.value as "ALL" | AlertSeverity)} value={severity}>
          <option value="ALL">All severities</option><option value="INFO">Info</option><option value="WARN">Warn</option><option value="ERROR">Error</option><option value="CRITICAL">Critical</option>
        </select>
        <input aria-label="Search alerts" className={managementInputClass} onChange={event => setSearch(event.target.value)} placeholder="Search message or fingerprint..." value={search} />
      </div>
      {center.loading ? <p className="p-4 text-sm text-muted">Loading alerts...</p> : null}
      {center.error ? <p className="m-4 rounded-md border border-error/30 bg-error/10 p-3 text-sm text-error">{center.error}</p> : null}
      {!center.loading ? <div className="divide-y divide-border">{filtered.map(alert => <article className="grid gap-3 p-4 lg:grid-cols-[12rem_1fr_10rem_auto] lg:items-center" key={alert.id}>
        <div><StatusBadge tone={severityTone(alert.severity)}>{alert.severity}</StatusBadge><p className="mt-2 text-xs text-muted">{alert.applicationDisplayName || alert.applicationName}</p></div>
        <button className="min-w-0 text-left" onClick={() => setSelected(alert)} type="button"><p className="truncate font-medium text-text">{alert.message}</p><p className="mt-1 text-xs text-muted">{formatDate(alert.triggeredAt)} · {alert.fingerprint.slice(0, 16)}</p></button>
        <StatusBadge tone={statusTone(alert.status)}>{alert.status}</StatusBadge>
        <div className="flex justify-end gap-2">{alert.status === "OPEN" ? <button className={managementButtonClass} disabled={center.saving} onClick={() => void center.acknowledge(alert.id)} type="button">Acknowledge</button> : null}{alert.status !== "RESOLVED" ? <button className={managementButtonClass} disabled={center.saving} onClick={() => void center.resolve(alert.id)} type="button">Resolve</button> : null}</div>
      </article>)}{filtered.length === 0 ? <p className="px-5 py-10 text-center text-sm text-muted">No alerts match the current filters.</p> : null}</div> : null}
    </section>
    {selected ? <section className={`${managementPanelClass} p-5`}><div className="flex justify-between gap-4"><h2 className="font-semibold text-text">Alert details</h2><button className={managementButtonClass} onClick={() => setSelected(null)} type="button">Close</button></div><dl className="mt-4 grid gap-3 text-sm md:grid-cols-2"><Detail label="Application" value={selected.applicationDisplayName || selected.applicationName}/><Detail label="Status" value={selected.status}/><Detail label="Triggered" value={formatDate(selected.triggeredAt)}/><Detail label="Log timestamp" value={formatDate(selected.logTimestamp)}/><Detail label="Fingerprint" value={selected.fingerprint}/><Detail label="Rule ID" value={selected.ruleId}/></dl><pre className="mt-4 overflow-x-auto whitespace-pre-wrap rounded-md border border-border bg-background p-4 text-sm text-text">{selected.message}</pre></section> : null}
  </div>;
}

function Detail({ label, value }: { label: string; value: string }) { return <div><dt className="text-xs uppercase text-muted">{label}</dt><dd className="mt-1 break-all text-text">{value}</dd></div>; }
function formatDate(value: string) { return new Date(value).toLocaleString(); }
function severityTone(value: AlertSeverity): "muted" | "warning" | "error" { return value === "INFO" ? "muted" : value === "WARN" ? "warning" : "error"; }
function statusTone(value: AlertStatus): "error" | "warning" | "success" { return value === "OPEN" ? "error" : value === "ACKNOWLEDGED" ? "warning" : "success"; }
