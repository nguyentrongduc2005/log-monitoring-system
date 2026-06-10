import type {
  PipelineState,
  PipelineStep
} from "@/features/dashboard/overview-types";

const stateClasses: Record<PipelineState, string> = {
  healthy: "text-success border-success/30 bg-success/10",
  degraded: "text-warning border-warning/30 bg-warning/10",
  delayed: "text-warning border-warning/30 bg-warning/10",
  offline: "text-error border-error/30 bg-error/10",
  unknown: "text-muted border-border bg-surface-raised"
};

export default function PipelineStrip({ steps }: { steps: PipelineStep[] }) {
  return (
    <section className="rounded-2xl border border-border bg-surface p-5">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-text">Pipeline flow</h2>
          <p className="mt-1 text-sm text-muted">
            Ingestion path from raw receipt to storage, streaming, and alerting.
          </p>
        </div>
      </div>

      <div className="mt-5 grid gap-3 lg:grid-cols-6">
        {steps.map((step) => (
          <article
            className="rounded-xl border border-border bg-surface-raised p-4"
            key={step.id}
          >
            <div
              className={`inline-flex rounded-full border px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.14em] ${stateClasses[step.state]}`}
            >
              {step.state}
            </div>
            <h3 className="mt-3 text-sm font-semibold text-text">{step.label}</h3>
            <p className="mt-2 text-sm text-muted">{step.detail}</p>
          </article>
        ))}
      </div>
    </section>
  );
}
