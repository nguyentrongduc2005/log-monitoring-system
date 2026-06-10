import type { LiveLogEntry } from "@/features/live-logs/live-logs-types";

export default function LogDetailDrawer({
  entry,
  onClose
}: {
  entry: LiveLogEntry | null;
  onClose: () => void;
}) {
  if (!entry) {
    return null;
  }

  return (
    <aside
      aria-label="Log details"
      className="rounded-2xl border border-border bg-surface p-5"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-text">Log details</h2>
          <p className="mt-1 text-sm text-muted">{entry.applicationName}</p>
        </div>
        <button
          className="inline-flex min-h-10 items-center justify-center rounded-xl border border-border bg-surface-raised px-4 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
          onClick={onClose}
          type="button"
        >
          Close
        </button>
      </div>

      <dl className="mt-5 space-y-4 text-sm">
        <div>
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Message
          </dt>
          <dd className="mt-2 text-text">{entry.message}</dd>
        </div>
        <div>
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Event ID
          </dt>
          <dd className="mt-2 font-mono text-text">{entry.eventId || "n/a"}</dd>
        </div>
        <div>
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Ingestion ID
          </dt>
          <dd className="mt-2 font-mono text-text">
            {entry.ingestionId || "n/a"}
          </dd>
        </div>
        <div>
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Trace ID
          </dt>
          <dd className="mt-2 font-mono text-text">{entry.traceId || "n/a"}</dd>
        </div>
        <div>
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Attributes
          </dt>
          {entry.attributes ? (
            <div className="mt-2 space-y-2">
              {Object.entries(entry.attributes).map(([key, value]) => (
                <div
                  className="flex items-center justify-between rounded-xl border border-border bg-surface-raised px-3 py-2"
                  key={key}
                >
                  <span className="text-muted">{key}</span>
                  <span className="font-mono text-text">{value}</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="mt-2 text-muted">No attributes available.</p>
          )}
        </div>
      </dl>
    </aside>
  );
}
