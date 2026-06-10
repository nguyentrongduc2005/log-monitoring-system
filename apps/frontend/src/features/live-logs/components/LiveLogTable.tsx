import type { LiveLogEntry } from "@/features/live-logs/live-logs-types";

const levelClasses = {
  INFO: "bg-primary/15 text-primary",
  WARN: "bg-warning/15 text-warning",
  ERROR: "bg-error/15 text-error",
  CRITICAL: "bg-error text-white"
} as const;

export default function LiveLogTable({
  entries,
  onSelect
}: {
  entries: LiveLogEntry[];
  onSelect: (entry: LiveLogEntry) => void;
}) {
  if (entries.length === 0) {
    return (
      <p className="rounded-2xl border border-border bg-surface p-5 text-sm text-muted">
        No logs match the current filters.
      </p>
    );
  }

  return (
    <div className="overflow-hidden rounded-2xl border border-border bg-surface">
      <div className="overflow-x-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-surface-raised text-xs uppercase tracking-[0.16em] text-muted/70">
            <tr>
              <th className="px-4 py-3">Timestamp</th>
              <th className="px-4 py-3">Application</th>
              <th className="px-4 py-3">Level</th>
              <th className="px-4 py-3">Message</th>
              <th className="px-4 py-3">Trace ID</th>
              <th className="px-4 py-3">Source</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry) => (
              <tr
                className="cursor-pointer border-t border-border transition hover:bg-surface-raised"
                key={entry.id}
                onClick={() => onSelect(entry)}
              >
                <td className="px-4 py-3 text-muted">{entry.timestamp}</td>
                <td className="px-4 py-3 font-medium text-text">
                  {entry.applicationName}
                </td>
                <td className="px-4 py-3">
                  <span
                    className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${levelClasses[entry.level]}`}
                  >
                    {entry.level}
                  </span>
                </td>
                <td className="max-w-xl px-4 py-3 text-text">{entry.message}</td>
                <td className="px-4 py-3 font-mono text-xs text-muted">
                  {entry.traceId || "n/a"}
                </td>
                <td className="px-4 py-3 text-muted">
                  {[entry.source, entry.host].filter(Boolean).join(" / ") || "n/a"}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
