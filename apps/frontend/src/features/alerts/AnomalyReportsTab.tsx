import { useEffect, useState } from "react";
import { getAnomalyReports, resolveAnomalyReport } from "@/features/anomaly/anomaly-api";
import type { AnomalyReport } from "@/features/anomaly/anomaly-types";
import { EmptyState, ErrorState, LoadingRows, StatusPill } from "@/shared/components/enterprise-ui";
import { Button, CardContent, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/components/ui";
import { cn } from "@/shared/lib/utils";
import React, { type ReactNode } from "react";

type JsonRecord = Record<string, unknown>;

type LogSample = {
  level: string;
  message: string;
};

type MetricEvidence = {
  metricName: string;
  currentValue?: number | null;
  thresholdValue?: number | null;
  criticalThresholdValue?: number | null;
  unit?: string;
  avg?: number | null;
  max?: number | null;
  samples?: number | null;
  lastSeenAt?: string;
  severity?: string;
};

export function AnomalyReportsTab() {
  const [reports, setReports] = useState<AnomalyReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);
  const [resolvingReportId, setResolvingReportId] = useState<string | null>(null);

  useEffect(() => {
    async function load() {
      try {
        const data = await getAnomalyReports();
        setReports(data);
      } catch (e: unknown) {
        setError(e instanceof Error ? e.message : "Failed to load anomaly reports");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  async function handleResolve(reportId: string) {
    setResolvingReportId(reportId);
    setError(null);
    try {
      await resolveAnomalyReport(reportId);
      setReports(current => current.filter(report => report.id !== reportId));
      setSelectedReportId(current => current === reportId ? null : current);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to resolve anomaly report");
    } finally {
      setResolvingReportId(null);
    }
  }

  if (loading) {
    return <LoadingRows />;
  }

  if (error) {
    return <ErrorState message={error} title="Unable to load anomaly reports" />;
  }

  if (reports.length === 0) {
    return (
      <EmptyState
        description="No anomalies have been detected."
        title="All clear"
      />
    );
  }

  return (
    <CardContent className="overflow-x-auto p-0">
      <Table className="min-w-[58rem]">
        <TableHeader>
          <TableRow>
            <TableHead className="w-40">Status</TableHead>
            <TableHead>Title</TableHead>
            <TableHead className="w-48">Detected</TableHead>
            <TableHead className="w-48 text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {reports.map(report => {
            const isSelected = selectedReportId === report.id;
            const isResolved = report.status === "RESOLVED";
            return (
              <React.Fragment key={report.id}>
                <TableRow className={isSelected ? "bg-primary/10" : undefined}>
                  <TableCell>
                    <StatusPill tone={statusTone(report.status)}>
                      {report.status}
                    </StatusPill>
                  </TableCell>
                  <TableCell>
                    <button
                      aria-current={isSelected ? "true" : undefined}
                      aria-expanded={isSelected}
                      className={cn(
                        "min-w-0 rounded-md px-2 py-1.5 text-left transition hover:bg-surface-raised focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70",
                        isSelected ? "bg-primary/10 ring-1 ring-primary/30" : ""
                      )}
                      onClick={() => setSelectedReportId(isSelected ? null : report.id)}
                      type="button"
                    >
                      <p className="truncate text-sm font-medium text-text">
                        {report.title || `Anomaly detected for Alert ${report.alertId || 'Unknown'}`}
                      </p>
                    </button>
                  </TableCell>
                  <TableCell className="text-muted">
                    {formatRelativeTime ? formatRelativeTime(report.createdAt) : report.createdAt}
                  </TableCell>
                  <TableCell>
                    <div className="flex flex-wrap justify-end gap-2">
                      <Button variant="outline" size="xs" onClick={() => setSelectedReportId(isSelected ? null : report.id)}>
                        {isSelected ? "Hide Detail" : "View Detail"}
                      </Button>
                      {!isResolved ? (
                        <Button
                          variant="outline"
                          size="xs"
                          onClick={() => handleResolve(report.id)}
                          disabled={resolvingReportId === report.id}
                        >
                          {resolvingReportId === report.id ? "Resolving" : "Resolve"}
                        </Button>
                      ) : null}
                    </div>
                  </TableCell>
                </TableRow>
                {isSelected ? (
                  <TableRow>
                    <TableCell colSpan={4}>
                      <AnomalyReportDetail report={report} />
                    </TableCell>
                  </TableRow>
                ) : null}
              </React.Fragment>
            );
          })}
        </TableBody>
      </Table>
    </CardContent>
  );
}

function AnomalyReportDetail({ report }: { report: AnomalyReport }) {
  const evidence = parseJsonRecord(report.evidencePayloadJson);
  const aiResult = parseJsonRecord(report.aiResultJson);
  const logSamples = extractLogSamples(evidence);
  const breachedMetrics = extractMetricEvidence(evidence?.breachedMetrics);
  const normalMetrics = extractMetricEvidence(evidence?.normalMetrics);
  const recommendedActions = stringArray(evidence?.recommendedActions);
  const aiActions = stringArray(aiResult?.suggestedActions);
  const evidenceRefs = stringArray(aiResult?.evidenceRefs);
  const fingerprints = stringArray(evidence?.fingerprints);
  const traceIds = stringArray(evidence?.traceIds);
  const relatedLogIds = stringArray(evidence?.relatedLogIds);

  return (
    <div className="space-y-4 rounded-lg border border-border bg-background p-4">
      <div className="flex flex-wrap items-center gap-2">
        <p className="text-xs font-semibold uppercase text-muted">Anomaly report</p>
        <StatusPill tone={statusTone(report.status)}>{report.status}</StatusPill>
        <StatusPill tone={severityTone(report.severity)}>{report.severity}</StatusPill>
        {report.aiStatus && report.aiStatus !== "NOT_REQUESTED" && (
          <StatusPill tone={report.aiStatus === "PENDING" ? "warning" : report.aiStatus === "SUCCEEDED" ? "success" : "error"}>
            AI: {report.aiStatus}
          </StatusPill>
        )}
      </div>

      <ReportSection title="Executive summary">
        <div className="space-y-3">
          <p className="text-sm text-text">{report.summary || report.title}</p>
          {report.hypothesis ? <p className="text-sm text-muted">{report.hypothesis}</p> : null}
        </div>
      </ReportSection>

      <div className="grid gap-3 lg:grid-cols-4">
        <ReportFact label="Source" value={report.sourceType} />
        <ReportFact label="Rule" value={report.ruleName} />
        <ReportFact label="Occurrences" value={report.occurrenceCount} />
        <ReportFact label="Confidence" value={formatScore(report.confidenceScore ?? numberValue(evidence?.confidenceScore))} />
      </div>

      <ReportSection title="Timeline">
        <div className="grid gap-3 md:grid-cols-2 lg:grid-cols-4">
          <ReportFact label="Window start" value={formatDateTime(report.windowStart)} compact />
          <ReportFact label="Window end" value={formatDateTime(report.windowEnd)} compact />
          <ReportFact label="First seen" value={formatDateTime(report.firstSeenAt)} compact />
          <ReportFact label="Last seen" value={formatDateTime(report.lastSeenAt)} compact />
        </div>
      </ReportSection>

      {breachedMetrics.length > 0 ? (
        <ReportSection title="Breached metrics">
          <div className="grid gap-3 lg:grid-cols-2">
            {breachedMetrics.map(metric => (
              <MetricPanel key={`${metric.metricName}-${metric.lastSeenAt || metric.currentValue}`} metric={metric} />
            ))}
          </div>
        </ReportSection>
      ) : null}

      {normalMetrics.length > 0 ? (
        <ReportSection title="Other observed metrics">
          <div className="grid gap-2 md:grid-cols-2 lg:grid-cols-3">
            {normalMetrics.map(metric => (
              <ReportFact
                key={metric.metricName}
                label={metric.metricName}
                value={`${formatMetricValue(metric.currentValue)}${metric.unit || ""}`}
                compact
              />
            ))}
          </div>
        </ReportSection>
      ) : null}

      {logSamples.length > 0 ? (
        <ReportSection title="Log samples">
          <div className="space-y-2">
            {logSamples.map((sample, index) => (
              <div className="rounded-md border border-border bg-surface p-3" key={`${sample.level}-${index}`}>
                <StatusPill tone={severityTone(sample.level)}>{sample.level}</StatusPill>
                <p className="mt-2 break-words font-mono text-xs leading-5 text-text">{sample.message}</p>
              </div>
            ))}
          </div>
        </ReportSection>
      ) : null}

      {!aiResult && recommendedActions.length > 0 ? (
        <ReportSection title="Recommended actions">
          <OrderedList items={recommendedActions} />
        </ReportSection>
      ) : null}

      {aiResult ? (
        <ReportSection title="AI analysis">
          <div className="space-y-4">
            <div className="grid gap-3 lg:grid-cols-4">
              <ReportFact label="Model" value={textValue(aiResult.model) || "Unavailable"} compact />
              <ReportFact label="Confidence" value={textValue(aiResult.confidence) || formatScore(numberValue(aiResult.confidenceScore))} compact />
              <ReportFact label="Severity" value={textValue(aiResult.severity) || "Unspecified"} compact />
              <ReportFact label="Prompt" value={textValue(aiResult.promptVersion) || "Default"} compact />
            </div>
            {textValue(aiResult.summary) ? <NarrativeBlock title="Summary" text={textValue(aiResult.summary)} /> : null}
            {textValue(aiResult.likelyCause) ? <NarrativeBlock title="Likely cause" text={textValue(aiResult.likelyCause)} /> : null}
            {textValue(aiResult.severityReason) ? <NarrativeBlock title="Severity rationale" text={textValue(aiResult.severityReason)} /> : null}
            {aiActions.length > 0 ? <OrderedList items={aiActions} /> : null}
          </div>
        </ReportSection>
      ) : report.aiError ? (
        <ReportSection title="AI analysis">
          <p className="text-sm text-error">{report.aiError}</p>
        </ReportSection>
      ) : null}

      <ReportSection title="Technical references">
        <div className="grid gap-3 lg:grid-cols-2">
          <ReportFact label="Report ID" value={report.id} compact />
          <ReportFact label="Application ID" value={report.applicationId} compact />
          <ReportFact label="Alert ID" value={report.alertId || "Not linked"} compact />
          <ReportFact label="Fingerprint" value={report.fingerprint || textValue(evidence?.dimensionValue) || "Unavailable"} compact />
          <ReportFact label="Dimension" value={dimensionLabel(evidence)} compact />
          <ReportFact label="AI trigger" value={report.aiTriggerReason || "Not requested"} compact />
        </div>
        <ReferenceList title="Fingerprints" values={fingerprints} />
        <ReferenceList title="Trace IDs" values={traceIds} />
        <ReferenceList title="Related log IDs" values={relatedLogIds} />
        <ReferenceList title="AI evidence refs" values={evidenceRefs} />
      </ReportSection>
    </div>
  );
}

function ReportSection({ children, title }: { children: ReactNode; title: string }) {
  return (
    <section className="space-y-3 rounded-md border border-border bg-surface-raised/20 p-4">
      <h4 className="text-sm font-semibold text-text">{title}</h4>
      {children}
    </section>
  );
}

function ReportFact({
  compact = false,
  label,
  value
}: {
  compact?: boolean;
  label: string;
  value: ReactNode;
}) {
  return (
    <div className={cn("min-w-0 rounded-md border border-border bg-surface p-3", compact ? "py-2.5" : "")}>
      <p className="text-[10px] font-semibold uppercase text-muted">{label}</p>
      <p className="mt-1 break-words text-sm font-medium text-text">{value || "Unavailable"}</p>
    </div>
  );
}

function MetricPanel({ metric }: { metric: MetricEvidence }) {
  const threshold = metric.thresholdValue ?? metric.criticalThresholdValue;
  return (
    <div className="rounded-md border border-border bg-surface p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="break-words text-sm font-semibold text-text">{metric.metricName}</p>
          <p className="mt-1 text-xs text-muted">
            Current {formatMetricValue(metric.currentValue)}{metric.unit || ""}
            {threshold != null ? ` / threshold ${formatMetricValue(threshold)}${metric.unit || ""}` : ""}
          </p>
        </div>
        {metric.severity ? <StatusPill tone={severityTone(metric.severity)}>{metric.severity}</StatusPill> : null}
      </div>
      <div className="mt-3 grid gap-2 sm:grid-cols-4">
        <ReportFact label="Average" value={`${formatMetricValue(metric.avg)}${metric.unit || ""}`} compact />
        <ReportFact label="Maximum" value={`${formatMetricValue(metric.max)}${metric.unit || ""}`} compact />
        <ReportFact label="Samples" value={metric.samples ?? "Unavailable"} compact />
        <ReportFact label="Last seen" value={metric.lastSeenAt ? formatDateTime(metric.lastSeenAt) : "Unavailable"} compact />
      </div>
    </div>
  );
}

function NarrativeBlock({ text, title }: { text: string; title: string }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase text-muted">{title}</p>
      <p className="mt-1 whitespace-pre-wrap text-sm text-text">{text}</p>
    </div>
  );
}

function OrderedList({ items }: { items: string[] }) {
  return (
    <ol className="space-y-2">
      {items.map((item, index) => (
        <li className="flex gap-3 text-sm text-text" key={`${item}-${index}`}>
          <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary-hover">
            {index + 1}
          </span>
          <span>{item}</span>
        </li>
      ))}
    </ol>
  );
}

function ReferenceList({ title, values }: { title: string; values: string[] }) {
  if (values.length === 0) return null;
  return (
    <div className="mt-3">
      <p className="text-[10px] font-semibold uppercase text-muted">{title}</p>
      <div className="mt-2 flex flex-wrap gap-2">
        {values.map(value => (
          <span className="max-w-full break-all rounded-md border border-border bg-surface px-2 py-1 font-mono text-xs text-muted" key={value}>
            {value}
          </span>
        ))}
      </div>
    </div>
  );
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

function formatDateTime(value?: string | null) {
  if (!value) return "Unavailable";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

function formatScore(value?: number | null) {
  if (value == null || Number.isNaN(value)) return "Unavailable";
  return `${Math.round(value * 100)}%`;
}

function formatMetricValue(value?: number | null) {
  if (value == null || Number.isNaN(value)) return "Unavailable";
  return Number.isInteger(value) ? String(value) : value.toFixed(2);
}

function parseJsonRecord(value?: string | null): JsonRecord | null {
  if (!value || !value.trim()) return null;
  try {
    const parsed: unknown = JSON.parse(value);
    return isRecord(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function isRecord(value: unknown): value is JsonRecord {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function textValue(value: unknown) {
  return typeof value === "string" ? value.trim() : "";
}

function numberValue(value: unknown) {
  return typeof value === "number" && Number.isFinite(value) ? value : null;
}

function stringArray(value: unknown) {
  return Array.isArray(value)
    ? value.filter((item): item is string => typeof item === "string" && item.trim().length > 0)
    : [];
}

function extractLogSamples(evidence: JsonRecord | null): LogSample[] {
  const structuredSamples = evidence?.logSamples;
  if (Array.isArray(structuredSamples)) {
    const samples = structuredSamples
      .filter(isRecord)
      .map(sample => ({
        level: textValue(sample.level) || "UNKNOWN",
        message: textValue(sample.message)
      }))
      .filter(sample => sample.message.length > 0);
    if (samples.length > 0) return samples;
  }

  return stringArray(evidence?.sampleMessages).map(message => ({
    level: "UNKNOWN",
    message
  }));
}

function extractMetricEvidence(value: unknown): MetricEvidence[] {
  if (!Array.isArray(value)) return [];
  return value.filter(isRecord).map(metric => ({
    metricName: textValue(metric.metricName) || "Metric",
    currentValue: numberValue(metric.currentValue),
    thresholdValue: numberValue(metric.thresholdValue),
    criticalThresholdValue: numberValue(metric.criticalThresholdValue),
    unit: textValue(metric.unit),
    avg: numberValue(metric.avg),
    max: numberValue(metric.max),
    samples: numberValue(metric.samples),
    lastSeenAt: textValue(metric.lastSeenAt),
    severity: textValue(metric.severity)
  }));
}

function dimensionLabel(evidence: JsonRecord | null) {
  const type = textValue(evidence?.dimensionType);
  const value = textValue(evidence?.dimensionValue);
  if (!type && !value) return "Unavailable";
  if (!type) return value;
  if (!value) return type;
  return `${type}: ${value}`;
}

function statusTone(status: string) {
  if (status === "RESOLVED") return "muted";
  if (status === "PENDING" || status === "AI_PENDING") return "warning";
  if (status === "AI_FAILED") return "error";
  return "success";
}

function severityTone(severity: string) {
  if (severity === "CRITICAL" || severity === "ERROR") return "error";
  if (severity === "WARN" || severity === "WARNING" || severity === "UNKNOWN") return "warning";
  if (severity === "INFO") return "primary";
  return "muted";
}
