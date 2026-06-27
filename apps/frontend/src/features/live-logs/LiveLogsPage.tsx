import { useEffect, useRef, useState } from "react";
import {
  createLiveLogConnection,
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
import { useAuth } from "@/features/auth/auth-context";
import { PageHeader } from "@/shared/layouts/page-header-context";

const defaultFilters: LiveLogFiltersValue = {
  applicationId: "",
  level: "ALL",
  keyword: ""
};

const MAX_BUFFERED_LOGS = 1_000;

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
  const { session } = useAuth();
  const [snapshot, setSnapshot] = useState<LiveLogSnapshot>(getEmptySnapshot);
  const [filters, setFilters] = useState<LiveLogFiltersValue>(defaultFilters);
  const [selectedEntry, setSelectedEntry] = useState<LiveLogEntry | null>(null);
  const [detailCollapsed, setDetailCollapsed] = useState(false);
  const [paused, setPaused] = useState(false);
  const [stickyToLatest, setStickyToLatest] = useState(true);
  const [wrapLines, setWrapLines] = useState(false);
  const [cleared, setCleared] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const pausedRef = useRef(paused);

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

  useEffect(() => {
    pausedRef.current = paused;
  }, [paused]);

  useEffect(() => {
    if (loading || error || snapshot.applications.length === 0) {
      return;
    }

    if (!session?.accessToken) {
      return;
    }

    const selectedApplications = filters.applicationId
      ? snapshot.applications.filter(
          (application) => application.id === filters.applicationId
        )
      : snapshot.applications;

    if (selectedApplications.length === 0) {
      return;
    }

    let active = true;
    const connection = createLiveLogConnection({
      accessToken: session.accessToken,
      applications: selectedApplications,
      onLog: (entry) => {
        if (!active) {
          return;
        }

        if (pausedRef.current) {
          setSnapshot((current) => ({
            ...current,
            buffered: current.buffered + 1
          }));
          return;
        }

        setCleared(false);
        setSnapshot((current) => {
          const entries = [...current.entries, entry];
          const dropped = Math.max(0, entries.length - MAX_BUFFERED_LOGS);

          return {
            ...current,
            entries: entries.slice(dropped),
            dropped: current.dropped + dropped
          };
        });
      },
      onStateChange: (connectionState) => {
        if (!active) {
          return;
        }

        setSnapshot((current) => ({
          ...current,
          connectionState
        }));
      }
    });

    return () => {
      active = false;
      connection.disconnect();
    };
  }, [
    error,
    filters.applicationId,
    loading,
    session?.accessToken,
    snapshot.applications
  ]);

  const filteredEntries = cleared
    ? []
    : filterLiveLogEntries(snapshot.entries, filters);
  const overflowCount = Math.max(0, filteredEntries.length - MAX_VISIBLE_LOGS);
  const visibleEntries = filteredEntries.slice(-MAX_VISIBLE_LOGS);
  const effectiveConnectionState: LiveConnectionState = paused
    ? "paused"
    : snapshot.connectionState;
  const bufferedCount = snapshot.buffered + overflowCount;

  function resetFilters() {
    setFilters(defaultFilters);
  }

  function togglePause() {
    setPaused((current) => {
      const nextPaused = !current;

      if (!nextPaused) {
        setStickyToLatest(true);
        setSnapshot((snapshot) => ({
          ...snapshot,
          buffered: 0
        }));
      }

      return nextPaused;
    });
  }

  const hasActiveFilters =
    filters.applicationId !== "" ||
    filters.level !== "ALL" ||
    filters.keyword.trim() !== "";

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
              setDetailCollapsed(false);
              setStickyToLatest(true);
            }}
            onFollowLatest={() => setStickyToLatest(true)}
            onTogglePause={togglePause}
            onToggleWrap={() => setWrapLines((current) => !current)}
            paused={paused}
            stickyToLatest={stickyToLatest}
            wrapLines={wrapLines}
          />
        }
        title="Live Logs"
      />

      {loading ? (
        <section className="rounded-lg border border-border bg-surface p-5 text-sm text-muted">
          Loading live logs...
        </section>
      ) : null}

      {error ? (
        <section className="rounded-lg border border-border bg-surface p-5">
          <p className="text-sm text-error">{error}</p>
          <button
            className="mt-4 inline-flex min-h-10 items-center justify-center rounded-lg bg-primary px-4 text-sm font-medium text-primary-foreground transition hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={() => void loadSnapshot()}
            type="button"
          >
            Retry
          </button>
        </section>
      ) : null}

      {!loading && !error && snapshot.applications.length === 0 ? (
        <section className="rounded-lg border border-border bg-surface p-5 text-sm text-muted">
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
            <section className="rounded-lg border border-border bg-surface p-5">
              <p className="text-sm text-muted">
                No logs match the current filters.
              </p>
              <button
                className="mt-4 inline-flex min-h-10 items-center justify-center rounded-lg border border-border bg-surface-raised px-4 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
                onClick={resetFilters}
                type="button"
              >
                Reset filters
              </button>
            </section>
          ) : (
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
                expanded={!selectedEntry || detailCollapsed}
                entries={visibleEntries}
                keyword={filters.keyword}
                onSelect={(entry) => {
                  setSelectedEntry(entry);
                  setDetailCollapsed(false);
                  setStickyToLatest(false);
                }}
                onStickyChange={setStickyToLatest}
                selectedEntryId={selectedEntry?.id}
                stickyToLatest={stickyToLatest}
                wrapLines={wrapLines}
              />
              <LogDetailDrawer
                collapsed={detailCollapsed}
                entry={selectedEntry}
                onClose={() => {
                  setSelectedEntry(null);
                  setDetailCollapsed(false);
                }}
                onToggleCollapse={() =>
                  setDetailCollapsed((current) => !current)
                }
              />
            </div>
          )}
        </>
      ) : null}
    </div>
  );
}
