import type {
  LogLevelDistribution,
  NotificationSummary,
} from "@/features/dashboard/overview-types";

const levelClasses = {
  INFO: "bg-primary/50",
  WARN: "bg-warning/55",
  ERROR: "bg-error/55",
  CRITICAL: "bg-error",
} as const;

export default function NotificationHealthPanel({
  distribution,
  summary,
}: {
  distribution: LogLevelDistribution[];
  summary: NotificationSummary;
}) {
  return (
    <section className="rounded-lg border border-border bg-surface p-5">
      <div>
        <h2 className="text-lg font-semibold text-text">Alert delivery</h2>
        <p className="mt-1 text-sm text-muted">
          Notification health and severity distribution for the current window.
        </p>
      </div>

      <dl className="mt-5 grid gap-3 sm:grid-cols-2">
        <Metric label="Sent" value={summary.sent.toLocaleString()} />
        <Metric
          label="Failed"
          tone="warning"
          value={summary.failed.toLocaleString()}
        />
        <Metric
          label="Dedup suppressed"
          value={summary.dedupSuppressed.toLocaleString()}
        />
        <Metric label="Delivery rate" value={summary.deliveryRate} />
      </dl>

      <div className="mt-5 rounded-lg border border-border bg-surface-raised p-4">
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-sm font-semibold text-text">
            Level distribution
          </h3>
          <span className="text-xs text-muted">
            Last failure {summary.lastFailure}
          </span>
        </div>
        <div className="mt-4 space-y-3">
          {distribution.map((item) => (
            <div key={item.level}>
              <div className="mb-1.5 flex items-center justify-between text-xs">
                <span className="font-semibold text-muted">{item.level}</span>
                <span className="text-muted">
                  {item.count.toLocaleString()} logs · {item.percentage}%
                </span>
              </div>
              <div className="h-2 rounded-full bg-background">
                <div
                  className={`h-2 rounded-full ${levelClasses[item.level]}`}
                  style={{ width: `${item.percentage}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function Metric({
  label,
  tone = "neutral",
  value,
}: {
  label: string;
  tone?: "neutral" | "warning";
  value: string;
}) {
  return (
    <div className="rounded-lg border border-border bg-surface-raised p-4">
      <dt className="text-xs font-semibold uppercase text-muted">{label}</dt>
      <dd
        className={
          tone === "warning"
            ? "mt-2 text-xl font-semibold text-warning"
            : "mt-2 text-xl font-semibold text-text"
        }
      >
        {value}
      </dd>
    </div>
  );
}
