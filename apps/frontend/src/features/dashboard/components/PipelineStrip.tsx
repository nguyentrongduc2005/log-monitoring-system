import type {
  PipelineStep,
} from "@/features/dashboard/overview-types";

export default function PipelineStrip({ steps }: { steps: PipelineStep[] }) {
  return (
    <section className="rounded-lg border border-border bg-surface p-5">
      <style>{`
        @keyframes dashflow {
          to {
            stroke-dashoffset: -20;
          }
        }
        .animate-dashflow {
          animation: dashflow 1.2s linear infinite;
        }
      `}</style>
      <div>
        <h2 className="text-lg font-semibold text-text">Pipeline health</h2>
        <p className="mt-1 text-sm text-muted">
          Current status across ingest, Kafka, storage, realtime, and
          alerting.
        </p>
      </div>

      <div className="mt-6 flex flex-col xl:flex-row items-center justify-between gap-4 xl:gap-2">
        {steps.map((step, idx) => (
          <div key={step.id} className="flex flex-col xl:flex-row items-center w-full xl:w-auto xl:flex-1">
            <article
              className={`w-full rounded-lg border border-border bg-[#090b0c] p-4 relative overflow-hidden transition-all duration-300 hover:border-primary/40`}
            >
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs font-semibold text-[#8a8f98] uppercase tracking-wider">
                  {step.label}
                </span>
                <span className="flex h-2 w-2 relative">
                  <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${
                    step.state === "healthy" ? "bg-success" :
                    step.state === "offline" ? "bg-error" : "bg-warning"
                  }`} />
                  <span className={`relative inline-flex rounded-full h-2 w-2 ${
                    step.state === "healthy" ? "bg-success" :
                    step.state === "offline" ? "bg-error" : "bg-warning"
                  }`} />
                </span>
              </div>
              <p className="mt-3 text-xs text-[#62666d] line-clamp-2 min-h-8">
                {step.detail}
              </p>
              <div className={`mt-2 text-[10px] font-mono font-bold uppercase tracking-wider ${
                step.state === "healthy" ? "text-success" :
                step.state === "offline" ? "text-error" : "text-warning"
              }`}>
                {step.state}
              </div>
            </article>

            {idx < steps.length - 1 ? (
              <div className="flex items-center justify-center h-8 xl:h-auto w-full xl:w-8 shrink-0">
                <svg className="h-full w-4 xl:h-4 xl:w-full rotate-90 xl:rotate-0 text-[#23252a]" viewBox="0 0 40 10" fill="none" preserveAspectRatio="none">
                  <path d="M 0 5 H 40" stroke="currentColor" strokeWidth="2" strokeDasharray="5 5" className="animate-dashflow" />
                  <polygon points="35,2 40,5 35,8" fill="currentColor" />
                </svg>
              </div>
            ) : null}
          </div>
        ))}
      </div>
    </section>
  );
}
