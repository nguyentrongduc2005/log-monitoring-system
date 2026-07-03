import type { OverviewMetric } from "@/features/dashboard/overview-types";

const toneClasses = {
  neutral: "border-border bg-surface-raised text-text",
  success: "border-success/30 bg-success/10 text-success",
  warning: "border-warning/30 bg-warning/10 text-warning",
  error: "border-error/30 bg-error/10 text-error",
} as const;

function Sparkline({ tone }: { tone: "neutral" | "success" | "warning" | "error" }) {
  const paths = {
    neutral: "M 0 15 Q 15 5, 30 18 T 60 12 T 90 20 T 120 10 L 120 25 L 0 25 Z",
    success: "M 0 18 Q 15 22, 30 12 T 60 8 T 90 15 T 120 4 L 120 25 L 0 25 Z",
    warning: "M 0 10 Q 15 8, 30 18 T 60 12 T 90 22 T 120 15 L 120 25 L 0 25 Z",
    error: "M 0 22 Q 15 15, 30 8 T 60 18 T 90 4 T 120 20 L 120 25 L 0 25 Z",
  };

  const strokePaths = {
    neutral: "M 0 15 Q 15 5, 30 18 T 60 12 T 90 20 T 120 10",
    success: "M 0 18 Q 15 22, 30 12 T 60 8 T 90 15 T 120 4",
    warning: "M 0 10 Q 15 8, 30 18 T 60 12 T 90 22 T 120 15",
    error: "M 0 22 Q 15 15, 30 8 T 60 18 T 90 4 T 120 20",
  };

  const colors = {
    neutral: { stroke: "#5e6ad2", stop: "#5e6ad2" },
    success: { stroke: "#27a644", stop: "#27a644" },
    warning: { stroke: "#f59e0b", stop: "#f59e0b" },
    error: { stroke: "#ef4444", stop: "#ef4444" },
  };

  const activeColor = colors[tone];

  return (
    <div className="h-6 w-20 opacity-80 shrink-0 self-end">
      <svg className="h-full w-full" viewBox="0 0 120 25" preserveAspectRatio="none">
        <defs>
          <linearGradient id={`grad-${tone}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={activeColor.stop} stopOpacity="0.25" />
            <stop offset="100%" stopColor={activeColor.stop} stopOpacity="0.0" />
          </linearGradient>
        </defs>
        <path
          d={paths[tone]}
          fill={`url(#grad-${tone})`}
        />
        <path
          d={strokePaths[tone]}
          fill="none"
          stroke={activeColor.stroke}
          strokeWidth="1.5"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  );
}

export default function OverviewMetricCard({
  metric,
}: {
  metric: OverviewMetric;
}) {
  return (
    <article className={`rounded-lg border p-4 flex flex-col justify-between min-h-[110px] ${toneClasses[metric.tone]}`}>
      <div className="w-full">
        <div className="flex items-start justify-between gap-3">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-muted/70">
            {metric.label}
          </p>
          {metric.trend ? (
            <span className="shrink-0 rounded-full border border-current/20 px-1.5 py-0.5 text-[10px] font-medium leading-none">
              {metric.trend}
            </span>
          ) : null}
        </div>
        <p className="mt-2.5 text-2xl font-bold leading-none tracking-tight">{metric.value}</p>
      </div>
      <div className="mt-3 flex items-center justify-between gap-2 w-full">
        {metric.helper ? (
          <p className="text-[11px] text-muted/80 truncate flex-1">{metric.helper}</p>
        ) : <div className="flex-1" />}
        <Sparkline tone={metric.tone} />
      </div>
    </article>
  );
}
