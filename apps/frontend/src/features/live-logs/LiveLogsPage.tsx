import { useEffect, useState } from "react";
import {
  filterLiveLogEntries,
  getInitialLiveLogSnapshot,
  MAX_VISIBLE_LOGS
} from "@/features/live-logs/live-logs-adapter";
import LogDetailDrawer from "@/features/live-logs/components/LogDetailDrawer";
import LiveLogFilters from "@/features/live-logs/components/LiveLogFilters";
import LiveLogTable from "@/features/live-logs/components/LiveLogTable";
import LiveLogsToolbar from "@/features/live-logs/components/LiveLogsToolbar";
import type {
  LiveConnectionState,
  LiveLogEntry,
  LiveLogFilters as LiveLogFiltersValue,
  LiveLogSnapshot
} from "@/features/live-logs/live-logs-types";
import { PageHeader } from "@/shared/layouts/page-header-context";

const defaultFilters: LiveLogFiltersValue = {
  applicationId: "",
  level: "ALL",
  keyword: "",
  traceId: ""
};

function getEmptySnapshot(): LiveLogSnapshot {
  return {
    applications: [],
    entries: [],
    connectionState: "connecting",
    buffered: 0,
    dropped: 0
  };
}

export function Component() {
  const [snapshot, setSnapshot] = useState<LiveLogSnapshot>(getEmptySnapshot);
  const [filters, setFilters] = useState<LiveLogFiltersValue>(defaultFilters);
  const [selectedEntry, setSelectedEntry] = useState<LiveLogEntry | null>(null);
  const [paused, setPaused] = useState(false);
  const [cleared, setCleared] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadSnapshot() {
    setLoading(true);
    setError(null);

    try {
      const nextSnapshot = await getInitialLiveLogSnapshot();
      setSnapshot(nextSnapshot);
      setCleared(false);
    } catch {
      setError("Unable to load live logs right now. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => {
      void loadSnapshot();
    });
  }, []);

  const filteredEntries = cleared
    ? []
    : filterLiveLogEntries(snapshot.entries, filters);
  const overflowCount = Math.max(0, filteredEntries.length - MAX_VISIBLE_LOGS);
  const visibleEntries = filteredEntries.slice(0, MAX_VISIBLE_LOGS);
  const effectiveConnectionState: LiveConnectionState = paused
    ? "paused"
    : snapshot.connectionState;
  const bufferedCount = snapshot.buffered + overflowCount;

  function resetFilters() {
    setFilters(defaultFilters);
  }

  const hasActiveFilters =
    filters.applicationId !== "" ||
    filters.level !== "ALL" ||
    filters.keyword.trim() !== "" ||
    filters.traceId.trim() !== "";

  return (
    <div className="space-y-6">
      <PageHeader
        actions={
          <LiveLogsToolbar
            buffered={bufferedCount}
            connectionState={effectiveConnectionState}
            dropped={snapshot.dropped}
            onClear={() => {
              setCleared(true);
              setSelectedEntry(null);
            }}
            onTogglePause={() => setPaused((current) => !current)}
            paused={paused}
          />
        }
        title="Live Logs"
      />

      {loading ? (
        <section className="rounded-2xl border border-border bg-surface p-5 text-sm text-muted">
          Loading live logs...
        </section>
      ) : null}

      {error ? (
        <section className="rounded-2xl border border-border bg-surface p-5">
          <p className="text-sm text-error">{error}</p>
          <button
            className="mt-4 inline-flex min-h-10 items-center justify-center rounded-xl bg-primary px-4 text-sm font-medium text-black transition hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={() => void loadSnapshot()}
            type="button"
          >
            Retry
          </button>
        </section>
      ) : null}

      {!loading && !error && snapshot.applications.length === 0 ? (
        <section className="rounded-2xl border border-border bg-surface p-5 text-sm text-muted">
          No authorized applications are available for live monitoring yet.
        </section>
      ) : null}

      {!error && snapshot.applications.length > 0 ? (
        <>
          <LiveLogFilters
            applications={snapshot.applications}
            filters={filters}
            onChange={setFilters}
            onReset={resetFilters}
          />

          {hasActiveFilters && visibleEntries.length === 0 ? (
            <section className="rounded-2xl border border-border bg-surface p-5">
              <p className="text-sm text-muted">
                No logs match the current filters.
              </p>
              <button
                className="mt-4 inline-flex min-h-10 items-center justify-center rounded-xl border border-border bg-surface-raised px-4 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
                onClick={resetFilters}
                type="button"
              >
                Reset filters
              </button>
            </section>
          ) : (
            <div className="grid gap-6 xl:grid-cols-[minmax(0,1.45fr)_minmax(20rem,0.85fr)]">
              <div className="space-y-3">
                {effectiveConnectionState === "disconnected" ? (
                  <p className="text-sm text-warning">
                    Connection status: Disconnected. Existing rows remain visible.
                  </p>
                ) : null}
                {effectiveConnectionState === "error" ? (
                  <p className="text-sm text-error">
                    Connection status: Error. Existing rows remain visible.
                  </p>
                ) : null}
                <LiveLogTable
                  entries={visibleEntries}
                  onSelect={setSelectedEntry}
                />
              </div>
              <LogDetailDrawer
                entry={selectedEntry}
                onClose={() => setSelectedEntry(null)}
              />
            </div>
          )}
        </>
      ) : null}
    </div>
  );
}
