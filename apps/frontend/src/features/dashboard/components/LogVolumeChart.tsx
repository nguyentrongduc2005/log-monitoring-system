import type { LogVolumePoint } from "@/features/dashboard/overview-types";

const levelClasses = {
  INFO: "bg-primary/35",
  WARN: "bg-warning/55",
  ERROR: "bg-error/55",
  CRITICAL: "bg-error"
} as const;

const levels = ["INFO", "WARN", "ERROR", "CRITICAL"] as const;

export default function LogVolumeChart({ points }: { points: LogVolumePoint[] }) {
  const maxValue = Math.max(
    1,
    ...points.flatMap((point) => levels.map((level) => point[level]))
  );

  return (
    <section
      aria-label="Log volume by level"
      className="rounded-2xl border border-border bg-surface p-5"
    >
      <div>
        <h2 className="text-lg font-semibold text-text">Log volume by level</h2>
        <p className="mt-1 text-sm text-muted">
          Snapshot of the recent ingest rhythm across severity bands.
        </p>
      </div>

      <div className="mt-5 grid gap-4 md:grid-cols-5">
        {points.map((point) => (
          <div
            className="rounded-xl border border-border bg-surface-raised p-3"
            key={point.time}
          >
            <p className="text-xs uppercase tracking-[0.16em] text-muted/70">
              {point.time}
            </p>
            <div className="mt-4 space-y-2">
              {levels.map((level) => (
                <div className="space-y-1" key={level}>
                  <div className="flex items-center justify-between text-xs text-muted">
                    <span>{level}</span>
                    <span>{point[level]}</span>
                  </div>
                  <div className="h-2 rounded-full bg-background">
                    <div
                      className={`h-2 rounded-full ${levelClasses[level]}`}
                      style={{ width: `${(point[level] / maxValue) * 100}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
