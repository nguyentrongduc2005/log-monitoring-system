import type { CriticalAlertSummary } from "@/features/dashboard/overview-types";

export default function RecentCriticalAlerts({
  alerts,
}: {
  alerts: CriticalAlertSummary[];
}) {
  return (
    <section className="rounded-lg border border-border bg-surface p-5">
      <h2 className="text-lg font-semibold text-text">
        Recent critical alerts
      </h2>
      <p className="mt-1 text-sm text-muted">
        Urgent alert summaries across authorized applications.
      </p>

      {alerts.length === 0 ? (
        <p className="mt-5 rounded-lg border border-border bg-surface-raised p-4 text-sm text-muted">
          No critical alerts in the current window.
        </p>
      ) : (
        <div className="mt-5 space-y-3">
          {alerts.map((alert) => (
            <article
              className="rounded-lg border border-border bg-surface-raised p-4"
              key={alert.id}
            >
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span
                    className={`rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase ${
                      alert.severity === "CRITICAL"
                        ? "bg-error/15 text-error"
                        : "bg-warning/15 text-warning"
                    }`}
                  >
                    {alert.severity}
                  </span>
                  <span className="text-sm font-medium text-text">
                    {alert.application}
                  </span>
                </div>
                <span className="text-xs text-muted">{alert.lastSeen}</span>
              </div>
              <p className="mt-3 text-sm text-text">{alert.logSamples?.[0]?.message || "No alert message provided"}</p>
              <div className="mt-3 flex flex-wrap gap-4 text-xs text-muted">
                <span>{alert.occurrences} occurrences</span>
                <span>{alert.deliveryState}</span>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}
