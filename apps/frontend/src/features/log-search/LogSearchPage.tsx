import { useEffect, useState } from "react";
import { searchLogs } from "@/features/log-search/log-search-adapter";
import type {
  LogSearchEntry,
  LogSearchFilters,
  LogSearchLevel,
  LogSearchSnapshot,
} from "@/features/log-search/log-search-types";
import { PageHeader } from "@/shared/layouts/page-header-context";

const defaultFilters: LogSearchFilters = {
  query: "timeout",
  applicationId: "",
  level: "ALL",
  range: "15m",
};

const levelClasses: Record<LogSearchLevel, string> = {
  INFO: "text-[#5e6ad2] bg-[#5e6ad2]/10 border-[#5e6ad2]/20",
  WARN: "text-[#f59e0b] bg-[#f59e0b]/10 border-[#f59e0b]/20",
  ERROR: "text-[#ef4444] bg-[#ef4444]/10 border-[#ef4444]/20",
  CRITICAL: "text-[#ef4444] bg-[#ef4444]/20 border-[#ef4444] animate-live",
};

const severityRowStyles = {
  INFO: "border-l-[#5e6ad2]/70",
  WARN: "border-l-[#f59e0b]/70 bg-[#f59e0b]/2",
  ERROR: "border-l-[#ef4444]/80 bg-[#ef4444]/2",
  CRITICAL: "border-l-[#ef4444] bg-[#ef4444]/5"
} as const;

const quickQueries = [
  "traceId:trc-pay-8842",
  "orderId:ORD-8842",
  "payment timeout",
  "invoice retry",
];

function getEmptySnapshot(): LogSearchSnapshot {
  return {
    applications: [],
    summary: {
      totalMatches: 0,
      errorMatches: 0,
      criticalMatches: 0,
      uniqueTraces: 0,
      slowestDurationMs: 0,
    },
    buckets: [],
    results: [],
    relatedTrace: [],
  };
}

export function Component() {
  const [filters, setFilters] = useState(defaultFilters);
  const [snapshot, setSnapshot] = useState<LogSearchSnapshot>(getEmptySnapshot);
  const [selectedLogId, setSelectedLogId] = useState<string | undefined>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadLogs(
    nextSelectedLogId: string | null | undefined = selectedLogId,
    nextFilters = filters,
  ) {
    setLoading(true);
    setError(null);

    try {
      const selectedId = nextSelectedLogId ?? undefined;
      const nextSnapshot = await searchLogs(nextFilters, selectedId);
      const nextSelection =
        nextSnapshot.results.find((log) => log.id === selectedId)?.id ??
        nextSnapshot.results[0]?.id;

      setSnapshot(nextSnapshot);
      setSelectedLogId(nextSelection);
    } catch {
      setError("Unable to search logs right now. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => {
      void loadLogs();
    });
  }, []);

  const selectedLog =
    snapshot.results.find((log) => log.id === selectedLogId) ??
    snapshot.results[0] ??
    null;

  function updateFilters(nextFilters: Partial<LogSearchFilters>) {
    setFilters((current) => ({ ...current, ...nextFilters }));
  }

  function runSearch() {
    void loadLogs();
  }

  function resetFilters() {
    setFilters(defaultFilters);
    setSelectedLogId(undefined);
    void loadLogs(null, defaultFilters);
  }

  function applyQuickQuery(query: string) {
    const nextFilters = { ...filters, query };
    setFilters(nextFilters);
    setSelectedLogId(undefined);
    void loadLogs(null, nextFilters);
  }

  return (
    <div className="space-y-5">
      <PageHeader title="Log Search" />

      <section className="rounded-lg border border-border bg-surface p-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-sm text-muted">
              Find exact log events and inspect the trace context around a
              failure.
            </p>
          </div>
          <div className="flex gap-2">
            <button
              className="inline-flex min-h-9 items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-sm font-medium text-text transition hover:border-primary hover:text-primary"
              onClick={resetFilters}
              type="button"
            >
              Reset
            </button>
            <button
              className="inline-flex min-h-9 items-center justify-center rounded-md bg-primary px-4 text-sm font-semibold text-primary-foreground transition hover:bg-primary-hover"
              onClick={runSearch}
              type="button"
            >
              Search
            </button>
          </div>
        </div>

        <div className="mt-5 grid gap-4 xl:grid-cols-[minmax(0,1fr)_24rem]">
          <div>
            <label className="block">
              <span className="text-xs font-semibold uppercase text-muted">
                Query
              </span>
              <input
                aria-label="Search query"
                className="mt-2 w-full rounded-md border border-border bg-background px-4 py-3 font-mono text-sm text-text outline-none placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
                onChange={(event) =>
                  updateFilters({ query: event.target.value })
                }
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    runSearch();
                  }
                }}
                placeholder="timeout OR traceId:trc-pay-8842 OR orderId:ORD-8842"
                value={filters.query}
              />
            </label>

            <div className="mt-3 flex flex-wrap gap-2">
              {quickQueries.map((query) => (
                <button
                  className="rounded-md border border-border bg-background px-2.5 py-1.5 font-mono text-xs text-muted transition hover:border-primary hover:text-primary"
                  key={query}
                  onClick={() => applyQuickQuery(query)}
                  type="button"
                >
                  {query}
                </button>
              ))}
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-3 xl:grid-cols-1">
            <label className="block">
              <span className="text-xs font-semibold uppercase text-muted">
                Application
              </span>
              <select
                aria-label="Filter by application"
                className="mt-2 w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                onChange={(event) =>
                  updateFilters({ applicationId: event.target.value })
                }
                value={filters.applicationId}
              >
                <option value="">All apps</option>
                {snapshot.applications.map((application) => (
                  <option key={application.id} value={application.id}>
                    {application.name}
                  </option>
                ))}
              </select>
            </label>

            <label className="block">
              <span className="text-xs font-semibold uppercase text-muted">
                Level
              </span>
              <select
                aria-label="Filter by level"
                className="mt-2 w-full rounded-md border border-border bg-background px-3 py-2 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
                onChange={(event) =>
                  updateFilters({
                    level: event.target.value as LogSearchFilters["level"],
                  })
                }
                value={filters.level}
              >
                <option value="ALL">All levels</option>
                <option value="INFO">INFO</option>
                <option value="WARN">WARN</option>
                <option value="ERROR">ERROR</option>
                <option value="CRITICAL">CRITICAL</option>
              </select>
            </label>

            <div>
              <span className="text-xs font-semibold uppercase text-muted">
                Time range
              </span>
              <div className="mt-2 grid grid-cols-4 rounded-lg border border-border bg-background p-1">
                {(["15m", "1h", "6h", "24h"] as const).map((range) => (
                  <button
                    aria-pressed={filters.range === range}
                    className={`min-h-8 rounded-md text-sm font-medium transition ${
                      filters.range === range
                        ? "bg-primary text-primary-foreground"
                        : "text-muted hover:text-text"
                    }`}
                    key={range}
                    onClick={() => updateFilters({ range })}
                    type="button"
                  >
                    {range}
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4 text-sm">
          <div className="flex flex-wrap gap-2 text-muted">
            <span className="rounded-md border border-border bg-background px-2 py-1">
              Search in message, traceId, eventId, source, host, attributes
            </span>
            <span className="rounded-md border border-border bg-background px-2 py-1">
              {snapshot.results.length} results
            </span>
          </div>
          {loading ? <span className="text-muted">Searching...</span> : null}
        </div>
      </section>

      {error ? (
        <section className="rounded-lg border border-border bg-surface p-5">
          <p className="text-sm text-error">{error}</p>
        </section>
      ) : null}

      <section className="grid gap-5 xl:grid-cols-[minmax(0,1.25fr)_minmax(25rem,0.75fr)]">
        <LogResults
          results={snapshot.results}
          selectedLogId={selectedLog?.id}
          onSelect={(log) => {
            setSelectedLogId(log.id);
            void loadLogs(log.id);
          }}
        />
        <LogInspector log={selectedLog} relatedTrace={snapshot.relatedTrace} />
      </section>
    </div>
  );
}

function LogResults({
  results,
  selectedLogId,
  onSelect,
}: {
  results: LogSearchEntry[];
  selectedLogId?: string;
  onSelect: (log: LogSearchEntry) => void;
}) {
  return (
    <article className="rounded-lg border border-border bg-surface sticky top-6 self-start">
      <div className="flex items-center justify-between gap-3 border-b border-border px-4 py-3">
        <div>
          <h2 className="text-lg font-semibold text-text">Search results</h2>
          <p className="mt-1 text-sm text-muted">
            Select a log to inspect payload and trace context.
          </p>
        </div>
        <span className="text-sm text-muted">{results.length} logs</span>
      </div>

      {results.length === 0 ? (
        <p className="p-4 text-sm text-muted">No logs match this search.</p>
      ) : (
        <div className="max-h-[720px] overflow-auto">
          {results.map((log) => (
            <button
              className={`grid w-full gap-2 border-b border-border px-4 py-3 text-left transition last:border-0 hover:bg-surface-raised ${
                selectedLogId === log.id
                  ? "border-l-[3px] border-l-primary bg-primary/10"
                  : `border-l-[3px] ${severityRowStyles[log.level]} odd:bg-surface/5`
              }`}
              key={log.id}
              onClick={() => onSelect(log)}
              type="button"
            >
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-mono text-xs text-[#62666d]">
                  {log.timestamp}
                </span>
                <span className="text-xs font-semibold text-[#8a8f98]">
                  {log.applicationName}
                </span>
              </div>
              <p className="font-mono text-sm leading-6 text-[#f7f8f8] flex items-start gap-1.5 min-w-0">
                <span
                  className={`inline-flex shrink-0 items-center justify-center rounded border px-1 py-0.5 text-[9px] font-bold tracking-wider uppercase leading-none ${levelClasses[log.level]}`}
                >
                  {log.level}
                </span>
                <span className="break-words">
                  {log.message}
                </span>
              </p>
              <div className="flex flex-wrap gap-3 font-mono text-xs text-[#62666d]">
                <span>trace={log.traceId}</span>
                <span>event={log.eventId}</span>
                <span>host={log.host}</span>
              </div>
            </button>
          ))}
        </div>
      )}
    </article>
  );
}

function LogInspector({
  log,
  relatedTrace,
}: {
  log: LogSearchEntry | null;
  relatedTrace: LogSearchEntry[];
}) {
  if (!log) {
    return (
      <aside className="rounded-lg border border-border bg-surface p-5 text-sm text-muted">
        Select a log to inspect details.
      </aside>
    );
  }

  return (
    <aside className="space-y-5">
      <article className="rounded-lg border border-border bg-surface p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-text">Log detail</h2>
            <p className="mt-1 font-mono text-xs text-muted">{log.eventId}</p>
          </div>
          <span
            className={`rounded-md border px-2 py-1 text-xs font-semibold ${levelClasses[log.level]}`}
          >
            {log.level}
          </span>
        </div>

        <dl className="mt-5 grid gap-3 text-sm">
          <Detail label="Message" value={log.message} mono />
          <div className="grid gap-3 sm:grid-cols-2">
            <Detail label="Trace ID" value={log.traceId} mono />
            <Detail label="Span ID" value={log.spanId} mono />
            <Detail label="Source" value={log.source} />
            <Detail label="Host" value={log.host} mono />
            <Detail label="Duration" value={`${log.durationMs ?? 0}ms`} />
            <Detail label="Status" value={String(log.statusCode ?? "n/a")} />
          </div>
        </dl>
      </article>

      <article className="rounded-lg border border-border bg-surface p-5">
        <h2 className="text-lg font-semibold text-text">Error trace</h2>
        <p className="mt-1 text-sm text-muted">
          Logs with the same trace ID, ordered by time.
        </p>
        <ol className="mt-5 space-y-3">
          {relatedTrace.map((traceLog) => (
            <li
              className={`rounded-lg border p-3 ${
                traceLog.id === log.id
                  ? "border-primary bg-primary/10"
                  : "border-border bg-background"
              }`}
              key={traceLog.id}
            >
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-mono text-xs text-muted">
                  {traceLog.timestamp}
                </span>
                <span
                  className={`rounded-md border px-2 py-0.5 text-[11px] font-semibold ${levelClasses[traceLog.level]}`}
                >
                  {traceLog.level}
                </span>
                <span className="text-xs text-muted">{traceLog.source}</span>
              </div>
              <p className="mt-2 font-mono text-sm text-text">
                {traceLog.message}
              </p>
            </li>
          ))}
        </ol>
      </article>

      <article className="rounded-lg border border-border bg-surface p-5">
        <h2 className="text-lg font-semibold text-text">Attributes</h2>
        <div className="mt-4 grid gap-2">
          {Object.entries(log.attributes).map(([key, value]) => (
            <div
              className="flex items-center justify-between gap-3 rounded-md border border-border bg-background px-3 py-2 text-sm"
              key={key}
            >
              <span className="text-muted">{key}</span>
              <span className="truncate font-mono text-xs text-text">
                {value}
              </span>
            </div>
          ))}
        </div>
      </article>

      {log.stack ? (
        <article className="rounded-lg border border-border bg-surface p-5">
          <h2 className="text-lg font-semibold text-text">Stack trace</h2>
          <pre className="mt-4 overflow-auto rounded-lg border border-border bg-background p-3 font-mono text-xs leading-6 text-text">
            {log.stack.join("\n")}
          </pre>
        </article>
      ) : null}
    </aside>
  );
}

function Detail({
  label,
  mono = false,
  value,
}: {
  label: string;
  mono?: boolean;
  value: string;
}) {
  return (
    <div className="rounded-md border border-border bg-background p-3">
      <dt className="text-xs font-semibold uppercase text-muted">{label}</dt>
      <dd
        className={`mt-2 break-words text-sm text-text ${
          mono ? "font-mono" : ""
        }`}
      >
        {value}
      </dd>
    </div>
  );
}
