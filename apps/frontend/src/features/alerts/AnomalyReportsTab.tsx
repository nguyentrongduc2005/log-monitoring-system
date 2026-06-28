import { useEffect, useState } from "react";
import { getAnomalyReports } from "@/features/incidents/incident-api";
import type { IncidentAnomalyReport } from "@/features/incidents/incident-types";
import { EmptyState, ErrorState, LoadingRows, StatusPill } from "@/shared/components/enterprise-ui";
import { Button, CardContent, Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/components/ui";
import { cn } from "@/shared/lib/utils";
import React from "react";

export function AnomalyReportsTab() {
  const [reports, setReports] = useState<IncidentAnomalyReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);

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
            <TableHead>Evidence Summary</TableHead>
            <TableHead className="w-48">Detected</TableHead>
            <TableHead className="w-48 text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {reports.map(report => {
            const isSelected = selectedReportId === report.id;
            return (
              <React.Fragment key={report.id}>
                <TableRow className={isSelected ? "bg-primary/10" : undefined}>
                  <TableCell>
                    <StatusPill tone={report.status === "PENDING" ? "warning" : "success"}>
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
                        Anomaly detected for Alert {report.alertId}
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

function AnomalyReportDetail({ report }: { report: IncidentAnomalyReport }) {
  let parsedPayload: unknown = null;
  try {
    parsedPayload = JSON.parse(report.evidencePayload);
  } catch {
    // Ignore parse error
  }

  return (
    <div className="rounded-lg border border-border bg-background p-4">
      <div className="flex flex-wrap items-center gap-2 mb-4">
        <p className="text-xs font-semibold uppercase tracking-[0.04em] text-muted">Report details</p>
        <StatusPill tone={report.status === "PENDING" ? "warning" : "success"}>{report.status}</StatusPill>
      </div>

      <div className="space-y-4">
        <div>
          <h4 className="text-sm font-semibold mb-2">Raw Evidence Payload</h4>
          <pre className="bg-surface p-4 rounded text-xs font-mono overflow-auto max-h-64 border border-border text-muted">
            {parsedPayload ? JSON.stringify(parsedPayload, null, 2) : report.evidencePayload}
          </pre>
        </div>
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
