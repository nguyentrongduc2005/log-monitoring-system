import type { LiveLogEntry } from "@/features/live-logs/live-logs-types";

export default function LogDetailDrawer({
  entry,
  collapsed,
  onToggleCollapse,
  onClose
}: {
  entry: LiveLogEntry | null;
  collapsed: boolean;
  onToggleCollapse: () => void;
  onClose: () => void;
}) {
  if (!entry) {
    return null;
  }

  if (collapsed) {
    return (
      <aside
        aria-label="Log details"
        className="flex items-center justify-between gap-3 rounded-lg border border-border bg-surface px-3 py-2"
      >
        <div className="min-w-0">
          <p className="truncate font-mono text-xs text-muted">
            {entry.timestamp} / {entry.applicationName} / {entry.level}
          </p>
          <p className="mt-0.5 truncate text-sm text-text">{entry.message}</p>
        </div>
        <div className="flex shrink-0 items-center gap-2">
          <button
            className="inline-flex min-h-8 items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={onToggleCollapse}
            type="button"
          >
            Expand
          </button>
          <button
            className="inline-flex min-h-8 items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={onClose}
            type="button"
          >
            Close
          </button>
        </div>
      </aside>
    );
  }

  return (
    <aside
      aria-label="Log details"
      className="rounded-lg border border-border bg-surface"
    >
      <div className="flex items-center justify-between gap-3 border-b border-border px-3 py-2">
        <div>
          <h2 className="text-sm font-semibold text-text">Log details</h2>
          <p className="mt-0.5 font-mono text-xs text-muted">
            {entry.applicationName} / {entry.level}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            className="inline-flex min-h-8 items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={onToggleCollapse}
            type="button"
          >
            Collapse
          </button>
          <button
            className="inline-flex min-h-8 items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={onClose}
            type="button"
          >
            Close
          </button>
        </div>
      </div>

      <dl className="grid gap-3 p-3 text-sm xl:grid-cols-[minmax(0,1.4fr)_minmax(22rem,0.9fr)]">
        <div className="rounded-md border border-border bg-background p-3">
          <dt className="text-xs uppercase text-muted/70">
            Message
          </dt>
          <dd className="mt-2 whitespace-pre-wrap font-mono text-text">
            {entry.message}
          </dd>
        </div>
        <div className="grid gap-3 md:grid-cols-3 xl:grid-cols-1">
          <div className="rounded-md border border-border bg-background p-3">
            <dt className="text-xs uppercase text-muted/70">Event ID</dt>
            <dd className="mt-2 truncate font-mono text-xs text-text">
              {entry.eventId || "n/a"}
            </dd>
          </div>
          <div className="rounded-md border border-border bg-background p-3">
            <dt className="text-xs uppercase text-muted/70">Ingestion ID</dt>
            <dd className="mt-2 truncate font-mono text-xs text-text">
              {entry.ingestionId || "n/a"}
            </dd>
          </div>
          <div className="rounded-md border border-border bg-background p-3">
            <dt className="text-xs uppercase text-muted/70">Trace ID</dt>
            <dd className="mt-2 truncate font-mono text-xs text-text">
              {entry.traceId || "n/a"}
            </dd>
          </div>
        </div>
        {entry.attributes ? (
          <div className="xl:col-span-2">
            <dt className="text-xs uppercase text-muted/70">Attributes</dt>
            <div className="mt-2 grid gap-2 md:grid-cols-2 xl:grid-cols-4">
              {Object.entries(entry.attributes).map(([key, value]) => (
                <div
                  className="flex items-center justify-between gap-3 rounded-md border border-border bg-background px-3 py-2"
                  key={key}
                >
                  <span className="truncate text-muted">{key}</span>
                  <span className="truncate font-mono text-xs text-text">
                    {value}
                  </span>
                </div>
              ))}
            </div>
          </div>
        ) : null}
      </dl>
    </aside>
  );
}
