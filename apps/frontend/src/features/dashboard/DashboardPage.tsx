import { useEffect, useState } from "react";
import { getOverviewSnapshot } from "@/features/dashboard/overview-adapter";
import LogVolumeChart from "@/features/dashboard/components/LogVolumeChart";
import OverviewMetricCard from "@/features/dashboard/components/OverviewMetricCard";
import RecentCriticalAlerts from "@/features/dashboard/components/RecentCriticalAlerts";
import type { OverviewSnapshot } from "@/features/dashboard/overview-types";
import { PageHeader } from "@/shared/layouts/page-header-context";

function createEmptySnapshot(): OverviewSnapshot {
  return {
    window: "Last 15 minutes",
    generatedAt: "",
    metrics: [],
    volume: [],
    criticalAlerts: [],
  };
}

export function Component() {
  const [snapshot, setSnapshot] =
    useState<OverviewSnapshot>(createEmptySnapshot);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadSnapshot() {
    setLoading(true);
    setError(null);

    try {
      const nextSnapshot = await getOverviewSnapshot("24h");
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
  }, []);

  return (
    <div className="space-y-6">
      <PageHeader
        actions={
          <div className="flex flex-wrap items-center gap-2">
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

          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
            {snapshot.metrics.map((metric) => (
              <OverviewMetricCard key={metric.id} metric={metric} />
            ))}
          </section>



          <div className="grid gap-6 xl:grid-cols-2">
            <LogVolumeChart points={snapshot.volume} />
            <RecentCriticalAlerts alerts={snapshot.criticalAlerts} />
          </div>
        </>
      ) : null}
    </div>
  );
}
