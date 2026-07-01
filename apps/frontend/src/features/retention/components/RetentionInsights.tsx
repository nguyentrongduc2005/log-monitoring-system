import {
  managementPanelClass,
  StatusBadge
} from "@/shared/components/management-ui";
import type { RetentionJob } from "../retention-types";
import { formatDate, operationTone } from "./retention-ui";

export default function RetentionInsights({ jobs }: { jobs: RetentionJob[] }) {
  const nextRunAt = jobs[0]?.nextRunAt;

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
        <h2 className="text-sm font-semibold text-text">Policy Windows</h2>
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
                {job.enabled ? `${job.retentionDays} days` : "Disabled"}
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
              {job.recentOperation ? (
                <>
                  <div className="flex items-center justify-between gap-2">
                    <StatusBadge tone={operationTone(job.recentOperation.status)}>
                      {job.recentOperation.status}
                    </StatusBadge>
                    <span className="text-xs text-muted">
                      {formatDate(
                        job.recentOperation.finishedAt ??
                          job.recentOperation.startedAt
                      )}
                    </span>
                  </div>
                  <p className="mt-2 text-sm text-text">
                    {job.recentOperation.message}
                  </p>
                </>
              ) : (
                <>
                  <StatusBadge tone={operationTone("PENDING")}>
                    PENDING
                  </StatusBadge>
                  <p className="mt-2 text-sm text-text">
                    No retention run recorded for {job.label}.
                  </p>
                </>
              )}
            </div>
          ))}
        </div>
      </section>
    </aside>
  );
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
