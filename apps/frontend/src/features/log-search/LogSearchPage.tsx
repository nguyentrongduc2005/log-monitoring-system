import { useEffect, useState, useCallback } from "react";
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

  // Pagination state
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // Live tail state
  const [liveTail, setLiveTail] = useState(false);

  const loadLogs = useCallback(
    async (
      nextSelectedLogId: string | null | undefined = selectedLogId,
      nextFilters = filters,
    ) => {
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
    },
    [selectedLogId, filters],
  );

  useEffect(() => {
    queueMicrotask(() => {
      void loadLogs();
    });
  }, [loadLogs]);

  // Live tail interval
  useEffect(() => {
    if (!liveTail) return;
    const interval = setInterval(() => {
      void loadLogs(selectedLogId, filters);
    }, 5000);
    return () => clearInterval(interval);
  }, [liveTail, filters, selectedLogId, loadLogs]);

  const selectedLog =
    snapshot.results.find((log) => log.id === selectedLogId) ??
    snapshot.results[0] ??
    null;

  function updateFilters(nextFilters: Partial<LogSearchFilters>) {
    setFilters((current) => ({ ...current, ...nextFilters }));
    setPage(1);
  }



  function runSearch() {
    setPage(1);
    void loadLogs();
  }

  function resetFilters() {
    setFilters(defaultFilters);
    setSelectedLogId(undefined);
    setPage(1);
    void loadLogs(null, defaultFilters);
  }

  function applyQuickQuery(query: string) {
    const nextFilters = { ...filters, query };
    setFilters(nextFilters);
    setSelectedLogId(undefined);
    setPage(1);
    void loadLogs(null, nextFilters);
  }

  function copyToClipboard(text: string) {
    void navigator.clipboard.writeText(text);
  }

  // Sliced results for pagination
  const startIndex = (page - 1) * pageSize;
  const endIndex = page * pageSize;
  const paginatedResults = snapshot.results.slice(startIndex, endIndex);

  return (
    <div className="space-y-5">
      <PageHeader title="Log Search" />

      {/* Summary Statistics Dashboard */}
      <div className="grid gap-3 grid-cols-2 md:grid-cols-5">
        <div className="rounded-lg border border-border bg-surface p-2.5">
          <div className="text-[10px] font-semibold uppercase text-muted">Total Logs</div>
          <div className="mt-1 text-lg font-bold font-mono">{snapshot.summary.totalMatches}</div>
        </div>
        <div className="rounded-lg border border-border bg-surface p-2.5">
          <div className="text-[10px] font-semibold uppercase text-muted">Errors</div>
          <div className="mt-1 text-lg font-bold font-mono text-error">{snapshot.summary.errorMatches}</div>
        </div>
        <div className="rounded-lg border border-border bg-surface p-2.5 flex items-center justify-between">
          <div>
            <div className="text-[10px] font-semibold uppercase text-muted">Criticals</div>
            <div className="mt-1 text-lg font-bold font-mono text-[#ef4444]">{snapshot.summary.criticalMatches}</div>
          </div>
          {snapshot.summary.criticalMatches > 0 && (
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-error opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-error"></span>
            </span>
          )}
        </div>
        <div className="rounded-lg border border-border bg-surface p-2.5">
          <div className="text-[10px] font-semibold uppercase text-muted">Unique Traces</div>
          <div className="mt-1 text-lg font-bold font-mono text-[#5e6ad2]">{snapshot.summary.uniqueTraces}</div>
        </div>
        <div className="rounded-lg border border-border bg-surface p-2.5">
          <div className="text-[10px] font-semibold uppercase text-muted">Slowest Latency</div>
          <div className="mt-1 text-lg font-bold font-mono text-warning">{snapshot.summary.slowestDurationMs}ms</div>
        </div>
      </div>

      <section className="rounded-lg border border-border bg-surface p-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-sm text-muted">
              Find exact log events and inspect the trace context around a
              failure.
            </p>
          </div>
          <div className="flex items-center gap-4 flex-wrap">
            {/* Live Tail Switch */}
            <label className="inline-flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={liveTail}
                onChange={(e) => setLiveTail(e.target.checked)}
                className="sr-only peer"
              />
              <div className="relative w-9 h-5 bg-input peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary"></div>
              <span className="text-sm font-medium text-muted peer-checked:text-text flex items-center gap-1.5">
                Live Tail
                {liveTail && <span className="h-2 w-2 rounded-full bg-success animate-live"></span>}
              </span>
            </label>
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

      {/* Visual Histogram / Log Volume Distribution */}
      {snapshot.buckets && snapshot.buckets.length > 0 && (
        <section className="rounded-lg border border-border bg-surface p-3.5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h3 className="text-xs font-semibold uppercase text-muted">Log Volume Distribution</h3>
            </div>
            <div className="flex flex-wrap gap-3 text-[10px]">
              <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded bg-[#5e6ad2]"></span> INFO</span>
              <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded bg-[#f59e0b]"></span> WARN</span>
              <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded bg-[#ef4444]"></span> ERROR</span>
              <span className="flex items-center gap-1.5"><span className="h-2 w-2 rounded bg-destructive animate-live"></span> CRITICAL</span>
            </div>
          </div>
          <div className="mt-3 flex h-14 items-end gap-1.5 border-b border-border pb-1">
            {snapshot.buckets.map((bucket, index) => {
              const maxTotal = Math.max(1, ...snapshot.buckets.map(b => b.total));
              const infoHeight = (bucket.info / maxTotal) * 100;
              const warnHeight = (bucket.warn / maxTotal) * 100;
              const errorHeight = (bucket.error / maxTotal) * 100;
              const criticalHeight = (bucket.critical / maxTotal) * 100;

              return (
                <button
                  key={index}
                  className="group relative flex-1 flex flex-col justify-end h-full hover:bg-surface-raised/40 rounded-t transition cursor-pointer"
                  onClick={() => applyQuickQuery(bucket.time)}
                  type="button"
                  aria-label={`Filter logs by bucket ${bucket.time}`}
                >
                  <div className="flex flex-col w-full rounded-t overflow-hidden">
                    {criticalHeight > 0 && <div className="bg-destructive animate-live w-full" style={{ height: `${criticalHeight}%` }}></div>}
                    {errorHeight > 0 && <div className="bg-[#ef4444] w-full" style={{ height: `${errorHeight}%` }}></div>}
                    {warnHeight > 0 && <div className="bg-[#f59e0b] w-full" style={{ height: `${warnHeight}%` }}></div>}
                    {infoHeight > 0 && <div className="bg-[#5e6ad2] w-full" style={{ height: `${infoHeight}%` }}></div>}
                  </div>

                  {/* Tooltip */}
                  <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block bg-surface border border-border p-2 rounded-md shadow-lg text-xs font-mono z-10 whitespace-nowrap">
                    <div className="font-semibold text-text mb-1 border-b border-border/50 pb-0.5">{bucket.time}</div>
                    <div className="text-muted">Total: <span className="text-text font-bold">{bucket.total}</span></div>
                    <div className="text-[#5e6ad2]">Info: {bucket.info}</div>
                    <div className="text-[#f59e0b]">Warn: {bucket.warn}</div>
                    <div className="text-[#ef4444]">Error: {bucket.error}</div>
                    <div className="text-destructive font-bold">Critical: {bucket.critical}</div>
                  </div>
                </button>
              );
            })}
          </div>
          <div className="mt-2 flex justify-between text-[10px] font-mono text-muted">
            <span>{snapshot.buckets[0]?.time}</span>
            <span>{snapshot.buckets[snapshot.buckets.length - 1]?.time}</span>
          </div>
        </section>
      )}

      {error ? (
        <section className="rounded-lg border border-border bg-surface p-5">
          <p className="text-sm text-error">{error}</p>
        </section>
      ) : null}

      <section className="grid gap-5 xl:grid-cols-[minmax(0,1.25fr)_minmax(25rem,0.75fr)]">
        <LogResults
          results={paginatedResults}
          totalResults={snapshot.results.length}
          selectedLogId={selectedLog?.id}
          onSelect={(log) => {
            setSelectedLogId(log.id);
            void loadLogs(log.id);
          }}
          page={page}
          pageSize={pageSize}
          onPageChange={setPage}
          onPageSizeChange={(size) => {
            setPageSize(size);
            setPage(1);
          }}
        />
        <LogInspector
          log={selectedLog}
          relatedTrace={snapshot.relatedTrace}
          onFilterTrace={(traceId) => applyQuickQuery(traceId)}
          onFilterHost={(host) => applyQuickQuery(host)}
          onCopy={copyToClipboard}
        />
      </section>
    </div>
  );
}

function LogResults({
  results,
  totalResults,
  selectedLogId,
  onSelect,
  page,
  pageSize,
  onPageChange,
  onPageSizeChange,
}: {
  results: LogSearchEntry[];
  totalResults: number;
  selectedLogId?: string;
  onSelect: (log: LogSearchEntry) => void;
  page: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (size: number) => void;
}) {
  const totalPages = Math.ceil(totalResults / pageSize);
  const startNum = totalResults === 0 ? 0 : (page - 1) * pageSize + 1;
  const endNum = Math.min(page * pageSize, totalResults);

  return (
    <article className="rounded-lg border border-border bg-surface sticky top-6 self-start w-full">
      <div className="flex items-center justify-between gap-3 border-b border-border px-4 py-3">
        <div>
          <h2 className="text-lg font-semibold text-text">Search results</h2>
          <p className="mt-1 text-sm text-muted">
            Select a log to inspect payload and trace context.
          </p>
        </div>
        <span className="text-sm text-muted">{totalResults} logs</span>
      </div>

      {results.length === 0 ? (
        <p className="p-4 text-sm text-muted">No logs match this search.</p>
      ) : (
        <>
          <div className="max-h-[640px] overflow-auto">
            {results.map((log) => (
              <button
                className={`flex items-center gap-3 w-full border-b border-border/40 px-3 py-1.5 text-left transition last:border-0 hover:bg-surface-raised font-mono text-[11px] ${
                  selectedLogId === log.id
                    ? "border-l-[3px] border-l-primary bg-primary/10"
                    : `border-l-[3px] ${severityRowStyles[log.level]} odd:bg-surface/5`
                }`}
                key={log.id}
                onClick={() => onSelect(log)}
                type="button"
              >
                {/* Time part of timestamp (HH:MM:SS.mmm) */}
                <span className="shrink-0 text-[#62666d] w-20">
                  {log.timestamp.split(" ")[1] || log.timestamp}
                </span>

                {/* Level Badge */}
                <span
                  className={`inline-flex shrink-0 w-14 items-center justify-center rounded border px-1 py-0.5 text-[9px] font-bold tracking-wider uppercase leading-none ${levelClasses[log.level]}`}
                >
                  {log.level}
                </span>

                {/* App Name */}
                <span className="shrink-0 text-[#8a8f98] w-24 truncate font-semibold">
                  {log.applicationName}
                </span>

                {/* Message */}
                <span className="flex-1 text-[#f7f8f8] truncate" title={log.message}>
                  {log.message}
                </span>

                {/* Trace ID */}
                <span className="shrink-0 text-[#62666d] w-24 truncate text-right" title={log.traceId}>
                  {log.traceId ? `trc:${log.traceId}` : ""}
                </span>
              </button>
            ))}
          </div>

          {/* Pagination Controls */}
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border px-4 py-3 text-sm text-muted bg-surface-raised/40">
            <div>
              Showing <span className="text-text font-medium">{startNum}</span> to{" "}
              <span className="text-text font-medium">{endNum}</span> of{" "}
              <span className="text-text font-medium">{totalResults}</span> logs
            </div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <span>Show</span>
                <select
                  aria-label="Logs per page"
                  value={pageSize}
                  onChange={(e) => onPageSizeChange(Number(e.target.value))}
                  className="rounded border border-border bg-background px-1.5 py-1 text-xs text-text outline-none cursor-pointer"
                >
                  <option value="20">20</option>
                  <option value="50">50</option>
                  <option value="100">100</option>
                </select>
              </div>
              <div className="flex items-center gap-1">
                <button
                  onClick={() => onPageChange(page - 1)}
                  disabled={page <= 1}
                  className="inline-flex h-8 items-center justify-center rounded border border-border bg-background px-2.5 text-xs font-medium text-text transition hover:bg-surface-raised disabled:opacity-40 disabled:hover:bg-background cursor-pointer"
                  type="button"
                >
                  Previous
                </button>
                <button
                  onClick={() => onPageChange(page + 1)}
                  disabled={page >= totalPages}
                  className="inline-flex h-8 items-center justify-center rounded border border-border bg-background px-2.5 text-xs font-medium text-text transition hover:bg-surface-raised disabled:opacity-40 disabled:hover:bg-background cursor-pointer"
                  type="button"
                >
                  Next
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </article>
  );
}

function LogInspector({
  log,
  relatedTrace,
  onFilterTrace,
  onFilterHost,
  onCopy,
}: {
  log: LogSearchEntry | null;
  relatedTrace: LogSearchEntry[];
  onFilterTrace: (traceId: string) => void;
  onFilterHost: (host: string) => void;
  onCopy: (text: string) => void;
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
          <div className="flex items-center gap-2">
            <button
              onClick={() => onCopy(JSON.stringify(log, null, 2))}
              className="inline-flex items-center gap-1.5 rounded-md border border-border bg-surface-raised px-2.5 py-1 text-xs text-muted transition hover:border-primary hover:text-primary cursor-pointer"
              title="Copy entire log JSON"
              type="button"
            >
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
              </svg>
              Copy JSON
            </button>
            <span
              className={`rounded-md border px-2 py-1 text-xs font-semibold ${levelClasses[log.level]}`}
            >
              {log.level}
            </span>
          </div>
        </div>

        <dl className="mt-5 grid gap-3 text-sm">
          <Detail label="Message" value={log.message} mono onCopy={() => onCopy(log.message)} />
          <div className="grid gap-3 sm:grid-cols-2">
            <Detail
              label="Trace ID"
              value={log.traceId}
              mono
              onCopy={() => onCopy(log.traceId)}
              onClick={() => onFilterTrace(log.traceId)}
            />
            <Detail label="Span ID" value={log.spanId} mono onCopy={() => onCopy(log.spanId)} />
            <Detail label="Source" value={log.source} onCopy={() => onCopy(log.source)} />
            <Detail
              label="Host"
              value={log.host}
              mono
              onCopy={() => onCopy(log.host)}
              onClick={() => onFilterHost(log.host)}
            />
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
  onCopy,
  onClick,
}: {
  label: string;
  mono?: boolean;
  value: string;
  onCopy?: () => void;
  onClick?: () => void;
}) {
  return (
    <div className="rounded-md border border-border bg-background p-3 group relative w-full">
      <div className="flex items-center justify-between w-full">
        <dt className="text-xs font-semibold uppercase text-muted">{label}</dt>
        <div className="flex items-center gap-1.5 opacity-0 group-hover:opacity-100 transition">
          {onCopy && (
            <button
              onClick={(e) => {
                e.stopPropagation();
                onCopy();
              }}
              className="p-0.5 rounded text-muted hover:text-text hover:bg-surface-raised cursor-pointer"
              title="Copy to clipboard"
              type="button"
            >
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3" />
              </svg>
            </button>
          )}
        </div>
      </div>
      <dd
        className={`mt-2 break-words text-sm text-text ${
          mono ? "font-mono" : ""
        }`}
      >
        {onClick ? (
          <button
            onClick={onClick}
            className="text-left text-primary hover:underline hover:text-primary-hover font-semibold cursor-pointer"
            type="button"
          >
            {value}
          </button>
        ) : (
          <span>{value}</span>
        )}
      </dd>
    </div>
  );
}
