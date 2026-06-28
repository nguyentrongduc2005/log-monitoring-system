import { Fragment, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getIncidentError, startIncidentFromAlert } from "@/features/incidents/incident-api";
import {
  EmptyState,
  ErrorState,
  FilterBar,
  LoadingRows,
  MetricTile,
  PageSection,
  PageShell,
  SectionHeader,
  StatusPill
} from "@/shared/components/enterprise-ui";
import {
  Button,
  CardContent,
  Input,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger
} from "@/shared/components/ui";
import { PageHeader } from "@/shared/layouts/page-header-context";
import { cn } from "@/shared/lib/utils";
import { useAlertCenter } from "./alert-center-context";
import type { Alert, AlertConnectionState, AlertSeverity, AlertStatus } from "./alerts-types";
import { AnomalyReportsTab } from "./AnomalyReportsTab";

export function Component() {
  const center = useAlertCenter();
  const navigate = useNavigate();
  const [applicationId, setApplicationId] = useState("ALL");
  const [status, setStatus] = useState<"ALL" | AlertStatus>("OPEN");
  const [severity, setSeverity] = useState<"ALL" | AlertSeverity>("ALL");
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<Alert | null>(null);
  const [startingIncidentId, setStartingIncidentId] = useState<string | null>(null);
  const [incidentError, setIncidentError] = useState<string | null>(null);

  const summary = useMemo(() => {
    const open = center.alerts.filter(alert => alert.status === "OPEN").length;
    const critical = center.alerts.filter(alert => alert.severity === "CRITICAL" && alert.status !== "RESOLVED").length;
    const acknowledged = center.alerts.filter(alert => alert.status === "ACKNOWLEDGED").length;
    const applications = new Set(center.alerts.map(alert => alert.applicationId).filter(Boolean)).size;
    const latest = center.alerts.reduce<string | null>((current, alert) => {
      if (!current) return alert.triggeredAt;
      return new Date(alert.triggeredAt).getTime() > new Date(current).getTime() ? alert.triggeredAt : current;
    }, null);

    return { acknowledged, applications, critical, latest, open, total: center.alerts.length };
  }, [center.alerts]);

  const filtered = useMemo(() => center.alerts.filter(alert =>
    (applicationId === "ALL" || alert.applicationId === applicationId) &&
    (status === "ALL" || alert.status === status) &&
    (severity === "ALL" || alert.severity === severity) &&
    (!search.trim() || `${alert.logSamples?.join(' ')} ${alert.applicationName}`.toLowerCase().includes(search.trim().toLowerCase()))
  ), [applicationId, center.alerts, search, severity, status]);

  async function startIncident(alert: Alert) {
    setStartingIncidentId(alert.id);
    setIncidentError(null);
    try {
      const incident = await startIncidentFromAlert(alert.id);
      navigate(`/incidents?selected=${incident.id}`);
    } catch (error) {
      setIncidentError(getIncidentError(error, "Unable to start incident."));
    } finally {
      setStartingIncidentId(null);
    }
  }

  return (
    <PageShell>
      <PageHeader
        actions={<Button onClick={() => void center.refresh()} variant="outline">Refresh</Button>}
        title="Alerts"
      />

      <PageSection>
        <SectionHeader
          actions={
            <div className="flex items-center gap-2 rounded-md border border-border bg-background px-3 py-2 text-xs text-muted">
              <span className="h-1.5 w-1.5 rounded-full bg-primary" />
              {filtered.length} visible of {summary.total}
            </div>
          }
          description={
            <>
              Realtime triage across authorized applications. Last event{" "}
              {summary.latest ? formatRelativeTime(summary.latest) : "not available"}.
            </>
          }
          title={
            <div className="flex flex-wrap items-center gap-2">
              <span>Alert Dashboard</span>
              <ConnectionBadge state={center.connectionState} />
            </div>
          }
        />

        <Tabs defaultValue="alerts">
          <div className="px-4 pt-4 border-b border-border">
            <TabsList variant="line">
              <TabsTrigger value="alerts">Realtime Alerts</TabsTrigger>
              <TabsTrigger value="anomaly">Anomaly Reports</TabsTrigger>
            </TabsList>
          </div>

          <TabsContent value="alerts" className="m-0 border-none outline-none">
            <div className="grid gap-3 border-b border-border p-4 md:grid-cols-4">
              <MetricTile label="Open" tone={summary.open > 0 ? "error" : "muted"} value={summary.open} />
              <MetricTile label="Critical active" tone={summary.critical > 0 ? "error" : "muted"} value={summary.critical} />
              <MetricTile label="Acknowledged" tone="warning" value={summary.acknowledged} />
              <MetricTile label="Applications" tone="primary" value={summary.applications} />
            </div>

            <FilterBar className="md:grid-cols-[minmax(10rem,1fr)_11rem_11rem_minmax(14rem,1.4fr)]">
              <LabeledSelect
                label="Filter application"
                onValueChange={setApplicationId}
                placeholder="All applications"
                value={applicationId}
              >
                <SelectItem value="ALL">All applications</SelectItem>
                {center.applications.map(app => <SelectItem key={app.id} value={app.id}>{app.name}</SelectItem>)}
              </LabeledSelect>
              <LabeledSelect
                label="Filter alert status"
                onValueChange={(value) => setStatus(value as "ALL" | AlertStatus)}
                placeholder="Status"
                value={status}
              >
                <SelectItem value="OPEN">Open</SelectItem>
                <SelectItem value="ACKNOWLEDGED">Acknowledged</SelectItem>
                <SelectItem value="RESOLVED">Resolved</SelectItem>
                <SelectItem value="ALL">All statuses</SelectItem>
              </LabeledSelect>
              <LabeledSelect
                label="Filter alert severity"
                onValueChange={(value) => setSeverity(value as "ALL" | AlertSeverity)}
                placeholder="Severity"
                value={severity}
              >
                <SelectItem value="ALL">All severities</SelectItem>
                <SelectItem value="INFO">Info</SelectItem>
                <SelectItem value="WARN">Warn</SelectItem>
                <SelectItem value="ERROR">Error</SelectItem>
                <SelectItem value="CRITICAL">Critical</SelectItem>
              </LabeledSelect>
              <Input
                aria-label="Search alerts"
                onChange={event => setSearch(event.target.value)}
                placeholder="Search logs, application..."
                value={search}
              />
            </FilterBar>

            {center.loading ? <LoadingRows /> : null}
            {center.error ? <ErrorState message={center.error} title="Unable to load alerts" /> : null}
            {incidentError ? <ErrorState message={incidentError} title="Incident creation failed" /> : null}

            {!center.loading ? (
              <CardContent className="overflow-x-auto p-0">
                <Table className="min-w-[58rem]">
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-36">Severity</TableHead>
                      <TableHead>Signal</TableHead>
                      <TableHead className="w-40">Status</TableHead>
                      <TableHead className="w-32">Age</TableHead>
                      <TableHead className="w-64 text-right">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filtered.map(alert => {
                      const isSelected = selected?.id === alert.id;
                      return (
                        <Fragment key={alert.id}>
                          <TableRow className={isSelected ? "bg-primary/10" : undefined}>
                            <TableCell>
                              <StatusPill tone={severityTone(alert.severity)}>{alert.severity}</StatusPill>
                              <p className="mt-2 truncate text-xs text-muted">{alert.applicationDisplayName || alert.applicationName || "Unknown application"}</p>
                            </TableCell>
                            <TableCell>
                              <button
                                aria-current={isSelected ? "true" : undefined}
                                aria-expanded={isSelected}
                                className={cn(
                                  "min-w-0 rounded-md px-2 py-1.5 text-left transition hover:bg-surface-raised focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70",
                                  isSelected ? "bg-primary/10 ring-1 ring-primary/30" : ""
                                )}
                                onClick={() => setSelected(isSelected ? null : alert)}
                                type="button"
                              >
                                <p className="truncate text-sm font-medium text-text">{alert.ruleName || alert.logSamples?.[0]?.message || "No alert message provided"}</p>
                                <p className="mt-1 truncate font-mono text-xs text-muted">{formatDate(alert.triggeredAt)}</p>
                              </button>
                            </TableCell>
                            <TableCell><StatusPill tone={statusTone(alert.status)}>{alert.status}</StatusPill></TableCell>
                            <TableCell className="text-muted">{formatRelativeTime(alert.triggeredAt)}</TableCell>
                            <TableCell>
                              <div className="flex flex-wrap justify-end gap-2">
                                <Button disabled={startingIncidentId === alert.id} onClick={() => void startIncident(alert)} size="xs">
                                  {startingIncidentId === alert.id ? "Starting..." : "Start Incident"}
                                </Button>
                                {alert.status === "OPEN" ? (
                                  <Button disabled={center.saving} onClick={() => void center.acknowledge(alert.id)} size="xs" variant="outline">
                                    Acknowledge
                                  </Button>
                                ) : null}
                                {alert.status !== "RESOLVED" ? (
                                  <Button disabled={center.saving} onClick={() => void center.resolve(alert.id)} size="xs" variant="secondary">
                                    Resolve
                                  </Button>
                                ) : null}
                              </div>
                            </TableCell>
                          </TableRow>
                          {isSelected ? (
                            <TableRow>
                              <TableCell colSpan={5}>
                                <AlertDetails alert={alert} />
                              </TableCell>
                            </TableRow>
                          ) : null}
                        </Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
                {filtered.length === 0 ? (
                  <EmptyState
                    description={center.alerts.length > 0 ? "Adjust status, severity, application, or search terms." : "New alert signals will appear here as they arrive."}
                    title={center.alerts.length > 0 ? "No alerts match the current filters." : "No alerts have been received yet."}
                  />
                ) : null}
              </CardContent>
            ) : null}
          </TabsContent>

          <TabsContent value="anomaly" className="m-0 border-none outline-none">
            <AnomalyReportsTab />
          </TabsContent>
        </Tabs>
      </PageSection>
    </PageShell>
  );
}

function LabeledSelect({
  children,
  label,
  onValueChange,
  placeholder,
  value
}: {
  children: React.ReactNode;
  label: string;
  onValueChange: (value: string) => void;
  placeholder: string;
  value: string;
}) {
  return (
    <Select onValueChange={onValueChange} value={value}>
      <SelectTrigger aria-label={label} className="w-full">
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>{children}</SelectContent>
    </Select>
  );
}

function AlertDetails({ alert }: { alert: Alert }) {
  const channels = alert.dispatchedChannels?.filter(Boolean) ?? [];

  return (
    <div className="rounded-lg border border-border bg-background p-4">
      <div className="flex flex-wrap items-center gap-2">
        <p className="mr-auto text-xs font-semibold uppercase tracking-[0.04em] text-muted">Alert details</p>
        <StatusPill tone={severityTone(alert.severity)}>{alert.severity}</StatusPill>
        <StatusPill tone={statusTone(alert.status)}>{alert.status}</StatusPill>
      </div>
      <dl className="mt-4 grid gap-4 text-sm md:grid-cols-2 xl:grid-cols-4">
        <Detail label="Application" value={alert.applicationDisplayName || alert.applicationName || "Unknown application"} />
        <Detail label="Triggered" value={formatDate(alert.triggeredAt)} />

        {alert.acknowledgedAt ? <Detail label="Acknowledged" value={formatDate(alert.acknowledgedAt)} /> : null}
        {alert.resolvedAt ? <Detail label="Resolved" value={formatDate(alert.resolvedAt)} /> : null}
        <Detail label="Channels" value={channels.length > 0 ? channels.join(", ") : "No delivery channel recorded"} />
      </dl>
      <div className="mt-4">
        <p className="text-xs font-semibold uppercase tracking-[0.04em] text-muted mb-2">Log Samples</p>
        {alert.logSamples && alert.logSamples.length > 0 ? (
          <ul className="space-y-2">
            {alert.logSamples.map((sample, index) => (
              <li key={index} className="flex gap-3 rounded-md border border-border bg-surface p-3 items-start">
                <StatusPill tone={severityTone(sample.level as AlertSeverity)}>{sample.level}</StatusPill>
                <div className="flex-1 whitespace-pre-wrap text-sm font-mono text-text">
                  {sample.message}
                </div>
              </li>
            ))}
          </ul>
        ) : (
          <div className="rounded-md border border-border bg-surface p-3 text-sm text-muted italic">
            No alert message provided.
          </div>
        )}
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return <div><dt className="text-xs uppercase tracking-[0.04em] text-muted">{label}</dt><dd className="mt-1 break-words text-text">{value}</dd></div>;
}

function ConnectionBadge({ state }: { state: AlertConnectionState }) {
  const live = state === "live";
  const label = live ? "Live" : state === "connecting" ? "Connecting" : state === "error" ? "Realtime error" : "Disconnected";
  return <StatusPill tone={live ? "primary" : "muted"}>{label}</StatusPill>;
}

function formatDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Unknown" : date.toLocaleString();
}

function formatRelativeTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "unknown";
  const diffMs = Date.now() - date.getTime();
  const absMs = Math.abs(diffMs);
  const minutes = Math.round(absMs / 60_000);
  if (minutes < 1) return "just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}



function severityTone(value: AlertSeverity): "primary" | "muted" | "warning" | "error" {
  return value === "INFO" ? "muted" : value === "WARN" ? "warning" : value === "ERROR" ? "primary" : "error";
}

function statusTone(value: AlertStatus): "error" | "warning" | "success" {
  return value === "OPEN" ? "error" : value === "ACKNOWLEDGED" ? "warning" : "success";
}
