import type { DemoReadiness } from "@/features/dashboard/overview-types";

export default function DemoReadinessPanel({
  readiness
}: {
  readiness: DemoReadiness;
}) {
  return (
    <section className="rounded-2xl border border-border bg-surface p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-text">500 logs / 2 seconds</h2>
          <p className="mt-1 text-sm text-muted">
            Assignment demo readiness based on the current adapter snapshot.
          </p>
        </div>
        <span className="rounded-full bg-primary/15 px-3 py-1 text-xs font-semibold text-primary">
          {readiness.streamStatus}
        </span>
      </div>

      <dl className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
        <div className="rounded-xl border border-border bg-surface-raised p-4">
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Accepted
          </dt>
          <dd className="mt-2 text-xl font-semibold text-text">
            {readiness.accepted}
          </dd>
        </div>
        <div className="rounded-xl border border-border bg-surface-raised p-4">
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Rejected
          </dt>
          <dd className="mt-2 text-xl font-semibold text-text">
            {readiness.rejected}
          </dd>
        </div>
        <div className="rounded-xl border border-border bg-surface-raised p-4">
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Duration
          </dt>
          <dd className="mt-2 text-xl font-semibold text-text">
            {readiness.duration}
          </dd>
        </div>
        <div className="rounded-xl border border-border bg-surface-raised p-4">
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            P95 ACK latency
          </dt>
          <dd className="mt-2 text-xl font-semibold text-text">
            {readiness.p95AckLatency}
          </dd>
        </div>
        <div className="rounded-xl border border-border bg-surface-raised p-4">
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Buffered
          </dt>
          <dd className="mt-2 text-xl font-semibold text-text">
            {readiness.buffered}
          </dd>
        </div>
        <div className="rounded-xl border border-border bg-surface-raised p-4">
          <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
            Dropped
          </dt>
          <dd className="mt-2 text-xl font-semibold text-text">
            {readiness.dropped}
          </dd>
        </div>
      </dl>
    </section>
  );
}
