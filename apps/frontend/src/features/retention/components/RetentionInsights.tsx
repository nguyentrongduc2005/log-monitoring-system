import {
  managementPanelClass,
  StatusBadge
} from "@/shared/components/management-ui";
import type { RetentionJob } from "../retention-types";
import { formatDate, operationTone } from "./retention-ui";

export default function RetentionInsights({ jobs }: { jobs: RetentionJob[] }) {
  const totalStorage = jobs.reduce((sum, job) => sum + job.storageTb, 0);
  const nextRunAt = jobs[0]?.nextRunAt;
  const gradient = buildStorageGradient(jobs);

  return (
    <aside className="space-y-3">
      <section className={`${managementPanelClass} p-4`}>
        <p className="text-xs font-semibold uppercase tracking-wide text-muted">
          Next run scheduled
        </p>
        <p className="mt-2 font-mono text-sm font-semibold text-text">
          {formatDate(nextRunAt)}
        </p>
      </section>

      <section className={`${managementPanelClass} p-4`}>
        <h2 className="text-sm font-semibold text-text">Storage Distribution</h2>
        <div className="mt-4 flex items-center justify-center">
          <div
            aria-label="Storage distribution chart"
            className="grid size-36 place-items-center rounded-full"
            style={{ background: gradient }}
          >
            <div className="grid size-24 place-items-center rounded-full bg-surface text-center">
              <div>
                <p className="text-[10px] font-semibold uppercase text-muted">Total</p>
                <p className="text-xl font-semibold text-text">
                  {totalStorage.toFixed(1)} TB
                </p>
              </div>
            </div>
          </div>
        </div>
        <div className="mt-4 space-y-2">
          {jobs.map(job => (
            <div className="flex items-center justify-between gap-3" key={job.id}>
              <div className="flex min-w-0 items-center gap-2">
                <span
                  className={`size-2 rounded-full ${dotClass(job.logLevel)}`}
                />
                <span className="truncate text-xs text-text">{job.label}</span>
              </div>
              <span className="text-xs text-muted">
                {job.storageTb.toFixed(1)} TB ({job.storagePercent}%)
              </span>
            </div>
          ))}
        </div>
      </section>

      <section className={managementPanelClass}>
        <div className="border-b border-border bg-surface-raised/35 px-4 py-3">
          <h2 className="text-sm font-semibold text-text">Recent Operations</h2>
        </div>
        <div className="divide-y divide-border">
          {jobs.map(job => (
            <div className="p-4" key={job.id}>
              <div className="flex items-center justify-between gap-2">
                <StatusBadge tone={operationTone(job.recentOperation.status)}>
                  {job.recentOperation.status}
                </StatusBadge>
                <span className="text-xs text-muted">
                  {formatDate(job.recentOperation.occurredAt)}
                </span>
              </div>
              <p className="mt-2 text-sm text-text">{job.recentOperation.message}</p>
            </div>
          ))}
        </div>
      </section>
    </aside>
  );
}

function buildStorageGradient(jobs: RetentionJob[]) {
  if (jobs.length === 0) {
    return "conic-gradient(var(--color-border) 0deg 360deg)";
  }

  let start = 0;
  const segments = jobs.map(job => {
    const end = start + (job.storagePercent / 100) * 360;
    const segment = `${chartColor(job.logLevel)} ${start}deg ${end}deg`;
    start = end;
    return segment;
  });

  return `conic-gradient(${segments.join(", ")})`;
}

function chartColor(level: RetentionJob["logLevel"]) {
  if (level === "CRITICAL") {
    return "#ff6b6b";
  }
  if (level === "ERROR") {
    return "#ffb199";
  }
  if (level === "WARN") {
    return "#6ee7a8";
  }
  return "#9bb7ff";
}

function dotClass(level: RetentionJob["logLevel"]) {
  if (level === "CRITICAL") {
    return "bg-error";
  }
  if (level === "ERROR") {
    return "bg-warning";
  }
  if (level === "WARN") {
    return "bg-success";
  }
  return "bg-primary";
}
