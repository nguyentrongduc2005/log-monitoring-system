import { useEffect, useState } from "react";
import { getOverviewSnapshot } from "@/features/dashboard/overview-adapter";
import LogVolumeChart from "@/features/dashboard/components/LogVolumeChart";
import NoisyApplicationsTable from "@/features/dashboard/components/NoisyApplicationsTable";
import NotificationHealthPanel from "@/features/dashboard/components/NotificationHealthPanel";
import OverviewMetricCard from "@/features/dashboard/components/OverviewMetricCard";
import PipelineStrip from "@/features/dashboard/components/PipelineStrip";
import RecentCriticalAlerts from "@/features/dashboard/components/RecentCriticalAlerts";
import type {
  DashboardWindow,
  OverviewSnapshot,
} from "@/features/dashboard/overview-types";
import { PageHeader } from "@/shared/layouts/page-header-context";

function createEmptySnapshot(): OverviewSnapshot {
  return {
    window: "Last 15 minutes",
    generatedAt: "",
    metrics: [],
    pipeline: [],
    volume: [],
    levelDistribution: [],
    noisyApplications: [],
    criticalAlerts: [],
    notificationSummary: {
      sent: 0,
      failed: 0,
      dedupSuppressed: 0,
      deliveryRate: "0%",
      lastFailure: "None",
    },
    authorizedApplications: 0,
  };
}

const windows: DashboardWindow[] = ["15m", "1h", "6h", "24h"];

export function Component() {
  const [snapshot, setSnapshot] =
    useState<OverviewSnapshot>(createEmptySnapshot);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedWindow, setSelectedWindow] = useState<DashboardWindow>("15m");

  async function loadSnapshot() {
    setLoading(true);
    setError(null);

    try {
      const nextSnapshot = await getOverviewSnapshot(selectedWindow);
      setSnapshot(nextSnapshot);
    } catch {
      setError(
        "Unable to load the overview snapshot right now. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => {
      void loadSnapshot();
    });
  }, [selectedWindow]);

  return (
    <div className="space-y-6">
      <PageHeader
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <div className="inline-flex rounded-lg border border-border bg-surface-raised p-1">
              {windows.map((range) => (
                <button
                  aria-pressed={selectedWindow === range}
                  className={`min-h-8 rounded-md px-3 text-sm font-medium transition ${
                    selectedWindow === range
                      ? "bg-primary text-primary-foreground"
                      : "text-muted hover:text-text"
                  }`}
                  key={range}
                  onClick={() => setSelectedWindow(range)}
                  type="button"
                >
                  {range}
                </button>
              ))}
            </div>
            <button
              className="inline-flex min-h-10 items-center justify-center rounded-lg border border-border bg-surface-raised px-4 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
              onClick={() => void loadSnapshot()}
              type="button"
            >
              Refresh
            </button>
          </div>
        }
        title="Dashboard"
      />

      {loading ? (
        <section className="rounded-lg border border-border bg-surface p-5 text-sm text-muted">
          Loading overview...
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

      {!error ? (
        <>
          <section className="rounded-lg border border-border bg-surface p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold text-text">
                  Operations overview
                </h2>
                <p className="mt-1 text-sm text-muted">
                  All applications this account can access · {snapshot.window}
                </p>
              </div>
              {snapshot.generatedAt ? (
                <span className="text-sm text-muted">
                  Updated{" "}
                  {new Date(snapshot.generatedAt).toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </span>
              ) : null}
            </div>
          </section>

          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-6">
            {snapshot.metrics.map((metric) => (
              <OverviewMetricCard key={metric.id} metric={metric} />
            ))}
          </section>

          {snapshot.authorizedApplications === 0 ? (
            <section className="rounded-lg border border-border bg-surface p-5 text-sm text-muted">
              No authorized applications are available for this account yet.
            </section>
          ) : null}

          <PipelineStrip steps={snapshot.pipeline} />

          <div className="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(22rem,0.9fr)]">
            <LogVolumeChart points={snapshot.volume} />
            <NotificationHealthPanel
              distribution={snapshot.levelDistribution}
              summary={snapshot.notificationSummary}
            />
          </div>

          <div className="grid gap-6 xl:grid-cols-[minmax(0,1.25fr)_minmax(22rem,0.95fr)]">
            <NoisyApplicationsTable
              applications={snapshot.noisyApplications}
              authorizedApplications={snapshot.authorizedApplications}
            />
            <RecentCriticalAlerts alerts={snapshot.criticalAlerts} />
          </div>
        </>
      ) : null}
    </div>
  );
}
