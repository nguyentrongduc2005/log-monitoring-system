import type { OverviewMetric } from "@/features/dashboard/overview-types";

const toneClasses = {
  neutral: "border-border bg-surface-raised text-text",
  success: "border-success/30 bg-success/10 text-success",
  warning: "border-warning/30 bg-warning/10 text-warning",
  error: "border-error/30 bg-error/10 text-error"
} as const;

export default function OverviewMetricCard({
  metric
}: {
  metric: OverviewMetric;
}) {
  return (
    <article
      className={`rounded-2xl border p-4 ${toneClasses[metric.tone]}`}
    >
      <p className="text-xs uppercase tracking-[0.16em] text-muted/80">
        {metric.label}
      </p>
      <p className="mt-3 text-2xl font-semibold">{metric.value}</p>
      {metric.trend ? <p className="mt-2 text-sm">{metric.trend}</p> : null}
    </article>
  );
}
