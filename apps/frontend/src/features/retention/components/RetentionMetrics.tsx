import { managementPanelClass } from "@/shared/components/management-ui";
import type { RetentionJob } from "../retention-types";

export default function RetentionMetrics({ jobs }: { jobs: RetentionJob[] }) {
  const projectedDeletion = jobs.reduce(
    (sum, job) => sum + job.projectedDeletionTbPerMonth,
    0
  );
  const compressionSavings = jobs.reduce(
    (sum, job) => sum + job.compressionSavingsGbPerMonth,
    0
  );
  const infoJob = jobs.find(job => job.logLevel === "INFO");

  return (
    <div className="space-y-3">
      <section className="grid gap-3 md:grid-cols-2">
        <div className={`${managementPanelClass} p-4`}>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">
            Projected deletions
          </p>
          <p className="mt-2 text-2xl font-semibold text-primary">
            {projectedDeletion.toFixed(1)} TB / Mo
          </p>
        </div>
        <div className={`${managementPanelClass} p-4`}>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">
            Compression savings
          </p>
          <p className="mt-2 text-2xl font-semibold text-success">
            {compressionSavings.toLocaleString()} GB / Mo
          </p>
        </div>
      </section>

      <section className="border-l-4 border-primary bg-primary/10 px-4 py-3">
        <p className="text-sm text-text">
          <span className="font-semibold text-primary">Storage advisory:</span>{" "}
          Based on current ingestion rates, storage will reach 90% capacity in 14
          days. Consider reducing INFO log retention
          {infoJob ? ` from ${infoJob.retentionDays} days to 21 days` : ""} to
          avoid overage charges.
        </p>
      </section>
    </div>
  );
}
