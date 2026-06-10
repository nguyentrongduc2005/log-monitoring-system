import { useEffect, useState } from "react";
import { getOverviewSnapshot } from "@/features/dashboard/overview-adapter";
import DemoReadinessPanel from "@/features/dashboard/components/DemoReadinessPanel";
import LogVolumeChart from "@/features/dashboard/components/LogVolumeChart";
import NoisyApplicationsTable from "@/features/dashboard/components/NoisyApplicationsTable";
import OverviewMetricCard from "@/features/dashboard/components/OverviewMetricCard";
import PipelineStrip from "@/features/dashboard/components/PipelineStrip";
import RecentCriticalAlerts from "@/features/dashboard/components/RecentCriticalAlerts";
import type { OverviewSnapshot } from "@/features/dashboard/overview-types";
import { PageHeader } from "@/shared/layouts/page-header-context";

function createEmptySnapshot(): OverviewSnapshot {
  return {
    metrics: [],
    pipeline: [],
    volume: [],
    noisyApplications: [],
    criticalAlerts: [],
    demoReadiness: {
      accepted: 0,
      rejected: 0,
      duration: "0s",
      p95AckLatency: "0 ms",
      streamStatus: "Unavailable",
      buffered: 0,
      dropped: 0
    },
    authorizedApplications: 0
  };
}

export function Component() {
  const [snapshot, setSnapshot] = useState<OverviewSnapshot>(createEmptySnapshot);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadSnapshot() {
    setLoading(true);
    setError(null);

    try {
      const nextSnapshot = await getOverviewSnapshot();
      setSnapshot(nextSnapshot);
    } catch {
      setError("Unable to load the overview snapshot right now. Please try again.");
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
          <button
            className="inline-flex min-h-10 items-center justify-center rounded-xl border border-border bg-surface-raised px-4 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={() => void loadSnapshot()}
            type="button"
          >
            Refresh
          </button>
        }
        title="Overview"
      />

      {loading ? (
        <section className="rounded-2xl border border-border bg-surface p-5 text-sm text-muted">
          Loading overview...
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

      {!error ? (
        <>
          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
            {snapshot.metrics.map((metric) => (
              <OverviewMetricCard key={metric.id} metric={metric} />
            ))}
          </section>

          {snapshot.authorizedApplications === 0 ? (
            <section className="rounded-2xl border border-border bg-surface p-5 text-sm text-muted">
              No authorized applications are available for this account yet.
            </section>
          ) : null}

          <PipelineStrip steps={snapshot.pipeline} />

          <div className="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_minmax(22rem,0.9fr)]">
            <LogVolumeChart points={snapshot.volume} />
            <DemoReadinessPanel readiness={snapshot.demoReadiness} />
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
