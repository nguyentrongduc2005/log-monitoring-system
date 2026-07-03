import { managementPanelClass } from "@/shared/components/management-ui";
import type { RetentionJob } from "../retention-types";

export default function RetentionMetrics({ jobs }: { jobs: RetentionJob[] }) {
  const enabledCount = jobs.filter(job => job.enabled).length;
  const recentDeletedRows = jobs.reduce(
    (sum, job) => sum + (job.recentOperation?.affectedRows ?? 0),
    0
  );
  const shortestPolicy = jobs.reduce<RetentionJob | null>((current, job) => {
    if (!job.enabled) {
      return current;
    }
    if (!current || job.retentionDays < current.retentionDays) {
      return job;
    }
    return current;
  }, null);

  return (
    <div className="space-y-3">
      <section className="grid gap-3 md:grid-cols-2">
        <div className={`${managementPanelClass} p-4`}>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">
            Enabled policies
          </p>
          <p className="mt-2 text-2xl font-semibold text-primary">
            {enabledCount} / {jobs.length}
          </p>
        </div>
        <div className={`${managementPanelClass} p-4`}>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">
            Recently deleted
          </p>
          <p className="mt-2 text-2xl font-semibold text-success">
            {recentDeletedRows.toLocaleString()} rows
          </p>
        </div>
      </section>

      <section className="border-l-4 border-primary bg-primary/10 px-4 py-3">
        <p className="text-sm text-text">
          <span className="font-semibold text-primary">Storage advisory:</span>{" "}
          The next retention run will delete logs older than each enabled
          policy. The shortest active window is
          {shortestPolicy
            ? ` ${shortestPolicy.retentionDays} days for ${shortestPolicy.label}.`
            : " not active because all policies are disabled."}
        </p>
      </section>
    </div>
  );
}
