import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import { Link, useSearchParams } from "react-router-dom";
import {
  EmptyState,
  ErrorState,
  FilterBar,
  LoadingRows,
  PageSection,
  PageShell,
  SectionHeader,
  StatusPill,
} from "@/shared/components/enterprise-ui";
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/components/ui";
import { PageHeader } from "@/shared/layouts/page-header-context";
import { cn } from "@/shared/lib/utils";
import {
  getIncident,
  getIncidentApplications,
  getIncidentError,
  getIncidents,
  resolveIncident,
} from "./incident-api";
import type {
  IncidentApplication,
  IncidentDetail,
  IncidentEvidence,
  IncidentSeverity,
  IncidentStatus,
  IncidentSummary,
} from "./incident-types";

type Filters = {
  applicationId: string;
  status: "ALL" | IncidentStatus;
  severity: "ALL" | IncidentSeverity;
};

const defaultFilters: Filters = {
  applicationId: "ALL",
  status: "INVESTIGATING",
  severity: "ALL",
};

export function Component() {
  const [searchParams] = useSearchParams();
  const [applications, setApplications] = useState<IncidentApplication[]>([]);
  const [incidents, setIncidents] = useState<IncidentSummary[]>([]);
  const [selected, setSelected] = useState<IncidentDetail | null>(null);
  const [filters, setFilters] = useState<Filters>(defaultFilters);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const selectedIncidentId = searchParams.get("selected") ?? undefined;

  const loadIncidents = useCallback(async (nextFilters: Filters, nextSelectedId?: string) => {
    setLoading(true);
    setError(null);
    try {
      const [nextApplications, nextIncidents] = await Promise.all([
        getIncidentApplications(),
        getIncidents(toApiFilters(nextFilters)),
      ]);
      setApplications(nextApplications);
      setIncidents(nextIncidents);
      const selectedId = nextSelectedId ?? nextIncidents[0]?.id;
      if (selectedId) {
        const stillVisible = nextIncidents.some((incident) => incident.id === selectedId);
        setSelected(stillVisible ? await getIncident(selectedId) : null);
      } else {
        setSelected(null);
      }
    } catch (loadError) {
      setError(getIncidentError(loadError, "Unable to load incidents."));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    queueMicrotask(() => void loadIncidents(defaultFilters, selectedIncidentId));
  }, [loadIncidents, selectedIncidentId]);

  const selectedAppNames = useMemo(
    () => new Map(applications.map((application) => [application.id, application.name])),
    [applications],
  );

  const summary = useMemo(() => ({
    investigating: incidents.filter((incident) => incident.status === "INVESTIGATING").length,
    sev1: incidents.filter((incident) => incident.severity === "SEV1").length,
    mitigated: incidents.filter((incident) => incident.status === "MITIGATED").length,
    affectedApps: new Set(incidents.flatMap((incident) => incident.applicationIds)).size,
  }), [incidents]);

  function updateFilters(nextFilters: Partial<Filters>) {
    const merged = { ...filters, ...nextFilters };
    setFilters(merged);
    void loadIncidents(merged);
  }

  async function selectIncident(id: string) {
    setError(null);
    try {
      setSelected(await getIncident(id));
    } catch (selectError) {
      setError(getIncidentError(selectError, "Unable to load incident."));
    }
  }

  async function runDetailAction(action: (id: string) => Promise<IncidentDetail>) {
    if (!selected) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await action(selected.id);
      setSelected(updated);
      await loadIncidents(filters, updated.id);
    } catch (actionError) {
      setError(getIncidentError(actionError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <PageShell>
      <PageHeader
        actions={<Button onClick={() => void loadIncidents(filters, selected?.id ?? selectedIncidentId)} variant="outline">Refresh</Button>}
        title="Incidents"
      />

      {error ? <ErrorState message={error} title="Incident workspace error" /> : null}

      <PageSection>
        <SectionHeader
          actions={<Button asChild><Link to="/alerts">Start from alert</Link></Button>}
          description="Investigate alert-driven incidents, supporting evidence, impact, and resolution workflow."
          title="Incident Response"
        />

        <div className="flex flex-wrap items-center gap-4 border-b border-border bg-surface-raised/20 px-5 py-3 text-xs md:gap-6 md:text-sm">
          <div className="flex items-center gap-2">
            <span className="text-muted">Investigating</span>
            <span className={cn("inline-flex items-center justify-center rounded-full px-2 py-0.5 font-bold text-xs", summary.investigating > 0 ? "bg-error/15 text-error" : "bg-muted/15 text-muted")}>
              {summary.investigating}
            </span>
          </div>
          <div className="hidden h-4 w-px bg-border md:block" />
          <div className="flex items-center gap-2">
            <span className="text-muted">SEV1</span>
            <span className={cn("inline-flex items-center justify-center rounded-full px-2 py-0.5 font-bold text-xs", summary.sev1 > 0 ? "bg-error/15 text-error" : "bg-muted/15 text-muted")}>
              {summary.sev1}
            </span>
          </div>
          <div className="hidden h-4 w-px bg-border md:block" />
          <div className="flex items-center gap-2">
            <span className="text-muted">Mitigated</span>
            <span className="inline-flex items-center justify-center rounded-full bg-warning/15 px-2 py-0.5 text-xs font-bold text-warning">
              {summary.mitigated}
            </span>
          </div>
          <div className="hidden h-4 w-px bg-border md:block" />
          <div className="flex items-center gap-2">
            <span className="text-muted">Affected apps</span>
            <span className="inline-flex items-center justify-center rounded-full bg-primary/15 px-2 py-0.5 text-xs font-bold text-primary-hover">
              {summary.affectedApps}
            </span>
          </div>
        </div>

        <FilterBar className="lg:grid-cols-[minmax(12rem,1fr)_12rem_12rem]">
          <LabeledSelect
            label="Filter application"
            onValueChange={(value) => updateFilters({ applicationId: value })}
            placeholder="All applications"
            value={filters.applicationId}
          >
            <SelectItem value="ALL">All applications</SelectItem>
            {applications.map((application) => (
              <SelectItem key={application.id} value={application.id}>
                {application.name}
              </SelectItem>
            ))}
          </LabeledSelect>
          <LabeledSelect
            label="Filter incident status"
            onValueChange={(value) => updateFilters({ status: value as Filters["status"] })}
            placeholder="All statuses"
            value={filters.status}
          >
            <SelectItem value="ALL">All statuses</SelectItem>
            <SelectItem value="INVESTIGATING">Investigating</SelectItem>
            <SelectItem value="MITIGATED">Mitigated</SelectItem>
            <SelectItem value="RESOLVED">Resolved</SelectItem>
          </LabeledSelect>
          <LabeledSelect
            label="Filter incident severity"
            onValueChange={(value) => updateFilters({ severity: value as Filters["severity"] })}
            placeholder="All severities"
            value={filters.severity}
          >
            <SelectItem value="ALL">All severities</SelectItem>
            <SelectItem value="SEV1">SEV1</SelectItem>
            <SelectItem value="SEV2">SEV2</SelectItem>
            <SelectItem value="SEV3">SEV3</SelectItem>
            <SelectItem value="UNKNOWN">Unknown</SelectItem>
          </LabeledSelect>
        </FilterBar>

        <div className="grid min-h-[42rem] xl:h-[calc(100vh-22rem)] xl:min-h-[38rem] xl:grid-cols-[minmax(20rem,25rem)_minmax(0,1fr)]">
          <aside className="border-b border-border bg-background/30 xl:border-b-0 xl:border-r xl:h-full xl:overflow-y-auto shell-scrollbar">
            <IncidentList
              appNames={selectedAppNames}
              incidents={incidents}
              loading={loading}
              onSelect={(incident) => void selectIncident(incident.id)}
              selectedId={selected?.id}
            />
          </aside>
          <IncidentDetailPanel
            appNames={selectedAppNames}
            incident={selected}
            onResolve={() => void runDetailAction(resolveIncident)}
            saving={saving}
          />
        </div>
      </PageSection>
    </PageShell>
  );
}

function toApiFilters(filters: Filters): {
  applicationId?: string;
  status?: "" | IncidentStatus;
  severity?: "" | IncidentSeverity;
} {
  return {
    applicationId: filters.applicationId === "ALL" ? "" : filters.applicationId,
    status: filters.status === "ALL" ? "" : filters.status,
    severity: filters.severity === "ALL" ? "" : filters.severity,
  };
}

function LabeledSelect({
  children,
  label,
  onValueChange,
  placeholder,
  value,
}: {
  children: ReactNode;
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

function IncidentList({
  appNames,
  incidents,
  loading,
  onSelect,
  selectedId,
}: {
  appNames: Map<string, string>;
  incidents: IncidentSummary[];
  loading: boolean;
  onSelect: (incident: IncidentSummary) => void;
  selectedId?: string;
}) {
  if (loading) {
    return <LoadingRows />;
  }
  if (incidents.length === 0) {
    return (
      <EmptyState
        description="Try widening status, severity, or application filters."
        title="No incidents match the current filters."
      />
    );
  }
  return (
    <div>
      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <p className="text-xs font-semibold uppercase tracking-[0.04em] text-muted">Incident queue</p>
        <span className="text-xs text-muted">{incidents.length} total</span>
      </div>
      <div className="divide-y divide-border">
        {incidents.map((incident) => {
          const isSelected = selectedId === incident.id;
          const services = incident.applicationIds
            .map((id) => appNames.get(id) ?? id.slice(0, 8))
            .join(", ");
          return (
            <button
              aria-current={isSelected ? "true" : undefined}
              className={cn(
                "group relative block w-full px-3 py-2.5 text-left transition hover:bg-surface-raised/45 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70",
                isSelected ? "bg-primary/10" : "",
              )}
              key={incident.id}
              onClick={() => onSelect(incident)}
              type="button"
            >
              <span className={cn("absolute bottom-2 left-0 top-2 w-0.5 rounded-r", severityStripeClass(incident.severity))} />
              <div className="pl-1.5 space-y-1.5">
                <div className="flex items-start justify-between gap-3">
                  <p className="line-clamp-1 text-xs font-semibold text-text flex-1" title={incident.title}>
                    {incident.title}
                  </p>
                  <div className="flex shrink-0 items-center gap-1 scale-90 origin-right">
                    <StatusPill tone={severityTone(incident.severity)}>{incident.severity}</StatusPill>
                    <StatusPill tone={statusTone(incident.status)}>{statusLabel(incident.status)}</StatusPill>
                  </div>
                </div>
                <div className="flex items-center justify-between text-[11px] text-muted">
                  <span className="truncate max-w-[12rem]">{services || "No services"}</span>
                  <span>{formatShortDate(incident.startedAt)}</span>
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function IncidentDetailPanel({
  appNames,
  incident,
  onResolve,
  saving,
}: {
  appNames: Map<string, string>;
  incident: IncidentDetail | null;
  onResolve: () => void;
  saving: boolean;
}) {
  if (!incident) {
    return (
      <div className="flex min-h-[32rem] items-center justify-center bg-background/20 p-8 xl:h-full">
        <EmptyState
          description="Choose an item from the queue to inspect evidence, timeline, and impact."
          title="Select an incident to review the operational summary."
        />
      </div>
    );
  }
  const affectedServices = incident.applications
    .map((application) => appNames.get(application.applicationId) ?? application.applicationId.slice(0, 8));
  return (
    <div className="min-w-0 bg-background/20 xl:h-full xl:overflow-y-auto shell-scrollbar">
      <div className="border-b border-border bg-card px-5 py-5">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap gap-2">
              <StatusPill tone={severityTone(incident.severity)}>{incident.severity}</StatusPill>
              <StatusPill tone={statusTone(incident.status)}>{statusLabel(incident.status)}</StatusPill>
            </div>
            <h2 className="mt-3 break-words text-xl font-semibold tracking-[-0.4px] text-text">{incident.title}</h2>
          </div>
          {incident.status !== "RESOLVED" ? (
            <Button disabled={saving} onClick={onResolve} variant="outline" className="xl:shrink-0">
              Resolve
            </Button>
          ) : null}
        </div>
        <div className="mt-4 flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-border pt-4 text-xs">
          <div className="flex items-center gap-1.5">
            <span className="text-muted">Detected:</span>
            <span className="font-medium text-text">{formatShortDate(incident.startedAt)}</span>
          </div>
          <div className="hidden h-3 w-px bg-border sm:block" />
          <div className="flex items-center gap-1.5">
            <span className="text-muted">Updated:</span>
            <span className="font-medium text-text">{formatShortDate(incident.updatedAt)}</span>
          </div>
          <div className="hidden h-3 w-px bg-border sm:block" />
          <div className="flex items-center gap-1.5">
            <span className="text-muted">Services:</span>
            <div className="flex flex-wrap gap-1">
              {affectedServices.length ? (
                affectedServices.map((service) => (
                  <span key={service} className="rounded bg-surface-raised border border-border px-1.5 py-0.5 text-[10px] text-muted">
                    {service}
                  </span>
                ))
              ) : (
                <span className="text-muted">None</span>
              )}
            </div>
          </div>
          <div className="hidden h-3 w-px bg-border sm:block" />
          <div className="flex items-center gap-1.5">
            <span className="text-muted">Evidence:</span>
            <span className={cn("rounded-full px-2 py-0.5 text-[10px] font-semibold", incident.evidence.length > 0 ? "bg-error/15 text-error" : "bg-muted/15 text-muted")}>
              {incident.evidence.length}
            </span>
          </div>
          <div className="hidden h-3 w-px bg-border sm:block" />
          <div className="flex items-center gap-1.5">
            <span className="text-muted">Timeline:</span>
            <span className="rounded-full bg-primary/15 px-2 py-0.5 text-[10px] font-semibold text-primary-hover">
              {incident.timeline.length}
            </span>
          </div>
        </div>
      </div>

      <div className="space-y-5 p-5">
        <OverviewSection incident={incident} />
        <EvidenceList evidence={incident.evidence} />
        <Timeline events={incident.timeline} />
      </div>
    </div>
  );
}

function OverviewSection({ incident }: { incident: IncidentDetail }) {
  return (
    <Card>
      <SmallSectionHeader title="Operational overview" />
      <CardContent className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="space-y-4">
          <InfoBlock label="Summary" value={incident.shortSummary} />
          <InfoBlock label="User/system impact" value={incident.impact} />
          <InfoBlock label="Possible cause" value={incident.possibleCause || "Analysis has not identified a likely cause yet."} />
        </div>
        <div className="rounded-md border border-border bg-background p-3">
          <p className="text-xs font-semibold uppercase tracking-[0.04em] text-muted">Recommended actions</p>
          {incident.recommendedActions.length ? (
            <ol className="mt-3 space-y-2">
              {incident.recommendedActions.map((value, index) => (
                <li className="flex gap-2 text-sm text-text" key={`${value}-${index}`}>
                  <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-md bg-primary/10 text-xs font-semibold text-primary">
                    {index + 1}
                  </span>
                  <span className="min-w-0 break-words">{value}</span>
                </li>
              ))}
            </ol>
          ) : (
            <p className="mt-3 text-sm text-muted">Recommended actions will appear after analysis completes.</p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

function EvidenceList({ evidence }: { evidence: IncidentEvidence[] }) {
  return (
    <Card>
      <SmallSectionHeader title="Supporting Evidence" meta={`${evidence.length} items`} />
      <div className="divide-y divide-border">
        {evidence.map((item) => <EvidenceRow item={item} key={item.id} />)}
        {evidence.length === 0 ? <p className="p-4 text-sm text-muted">No evidence collected for this incident.</p> : null}
      </div>
    </Card>
  );
}

function EvidenceRow({ item }: { item: IncidentEvidence }) {
  const metadata = parseEvidenceMetadata(item.metadataJson);
  const sample = displayEvidenceSample(item);
  return (
    <article className="grid gap-3 p-4 transition hover:bg-surface-raised/45 lg:grid-cols-[9rem_minmax(0,1fr)_12rem] lg:items-start">
      <div className="flex flex-wrap gap-2 lg:block lg:space-y-2">
        <StatusPill tone={item.severity === "ERROR" || item.severity === "CRITICAL" ? "error" : "muted"}>
          {item.type}
        </StatusPill>
        <p className="text-xs text-muted font-medium">{item.severity || "UNKNOWN"}</p>
      </div>
      <div className="min-w-0">
        <p className="break-words font-semibold text-sm text-text">{item.summary}</p>
        <div className="mt-2 text-xs text-muted bg-surface/50 p-2 rounded border border-border/50">
          <p className="font-mono whitespace-pre-wrap">{sample || "No sample message captured."}</p>
        </div>
        {(item.fingerprint || metadata.traceIds.length > 0 || metadata.kind) ? (
          <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-muted">
            {metadata.kind ? (
              <span className="font-mono bg-background px-1.5 py-0.5 rounded border border-border">
                {metadata.kind}
              </span>
            ) : null}
            {metadata.traceIds.slice(0, 3).map(traceId => (
              <span className="font-mono bg-background px-1.5 py-0.5 rounded border border-border" key={traceId}>
                traceId: {traceId}
              </span>
            ))}
            {item.fingerprint ? (
            <span className="font-mono bg-background px-1.5 py-0.5 rounded border border-border">
              fingerprint: {item.fingerprint}
            </span>
            ) : null}
          </div>
        ) : null}
      </div>
      <p className="text-xs text-muted lg:text-right">{formatShortDate(item.occurredAt)}</p>
    </article>
  );
}

function displayEvidenceSample(item: IncidentEvidence) {
  const value = item.sampleMessage || "";
  const parsed = parseJson(value);
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    return value;
  }
  const record = parsed as Record<string, unknown>;
  const samples = Array.isArray(record.logSamples)
    ? record.logSamples
        .filter((sample): sample is Record<string, unknown> => Boolean(sample) && typeof sample === "object" && !Array.isArray(sample))
        .map(sample => textValue(sample.message))
        .filter(Boolean)
    : [];
  const count = numberValue(record.observedCount);
  const threshold = numberValue(record.thresholdCount);
  const hypothesis = textValue(record.hypothesis);
  const parts = [
    samples[0],
    count != null && threshold != null ? `${count} matching events exceeded threshold ${threshold}.` : "",
    hypothesis
  ].filter(Boolean);
  return parts.join("\n");
}

function parseEvidenceMetadata(value?: string | null) {
  const parsed = parseJson(value || "");
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    return { kind: "", traceIds: [] as string[] };
  }
  const record = parsed as Record<string, unknown>;
  const traceIds = Array.isArray(record.traceIds)
    ? record.traceIds.map(textValue).filter(Boolean)
    : textValue(record.traceId) ? [textValue(record.traceId)] : [];
  return {
    kind: textValue(record.kind),
    traceIds
  };
}

function parseJson(value: string) {
  if (!value.trim().startsWith("{")) return null;
  try {
    return JSON.parse(value) as unknown;
  } catch {
    return null;
  }
}

function textValue(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}

function numberValue(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function Timeline({ events }: { events: IncidentDetail["timeline"] }) {
  return (
    <Card>
      <SmallSectionHeader title="Timeline" meta={`${events.length} event${events.length === 1 ? "" : "s"}`} />
      <CardContent className="space-y-4">
        {events.map((event) => (
          <div className="border-l border-border pl-3" key={event.id}>
            <p className="text-sm font-medium text-text">{event.message}</p>
            <p className="mt-1 text-xs text-muted">{event.eventType} · {formatShortDate(event.createdAt)}</p>
          </div>
        ))}
        {events.length === 0 ? <p className="text-sm text-muted">No timeline events.</p> : null}
      </CardContent>
    </Card>
  );
}

function SmallSectionHeader({ title, meta }: { title: string; meta?: string }) {
  return (
    <CardHeader className="flex flex-row items-center justify-between gap-3 border-b border-border py-3">
      <CardTitle className="text-xs font-semibold uppercase tracking-[0.04em] text-muted">{title}</CardTitle>
      {meta ? <span className="text-xs text-muted">{meta}</span> : null}
    </CardHeader>
  );
}

function InfoBlock({ label, value }: { label: string; value?: string | null }) {
  if (!value) return null;
  return (
    <div className="rounded-md border border-border bg-background p-3">
      <p className="text-xs font-semibold uppercase tracking-[0.04em] text-muted">{label}</p>
      <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-text">{value}</p>
    </div>
  );
}

function formatShortDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unknown";
  return date.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function severityTone(value: IncidentSeverity): "muted" | "warning" | "error" {
  return value === "UNKNOWN" ? "muted" : value === "SEV3" ? "warning" : "error";
}

function severityStripeClass(value: IncidentSeverity) {
  return value === "UNKNOWN" ? "bg-muted" : value === "SEV3" ? "bg-warning" : "bg-error";
}

function statusTone(value: IncidentStatus): "error" | "warning" | "success" {
  return value === "RESOLVED" ? "success" : value === "MITIGATED" ? "warning" : "error";
}

function statusLabel(value: IncidentStatus) {
  return value === "INVESTIGATING" ? "Investigating" : value === "MITIGATED" ? "Mitigated" : "Resolved";
}

