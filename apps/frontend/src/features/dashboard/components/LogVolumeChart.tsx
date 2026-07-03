import ReactECharts from "echarts-for-react";
import type { LogVolumePoint } from "@/features/dashboard/overview-types";

const levelClasses = {
  INFO: "bg-primary/50",
  WARN: "bg-warning/65",
  ERROR: "bg-error/65",
  CRITICAL: "bg-error",
} as const;

const levels = ["INFO", "WARN", "ERROR", "CRITICAL"] as const;

const levelColors = {
  INFO: "#5e6ad2",
  WARN: "#f59e0b",
  ERROR: "#ef4444",
  CRITICAL: "#fb3d46",
} as const;

export default function LogVolumeChart({
  points,
}: {
  points: LogVolumePoint[];
}) {
  const option = {
    backgroundColor: "transparent",
    color: levels.map((level) => levelColors[level]),
    grid: {
      left: 44,
      right: 18,
      top: 28,
      bottom: 34,
    },
    legend: {
      data: levels,
      icon: "circle",
      itemHeight: 9,
      itemWidth: 9,
      right: 0,
      textStyle: {
        color: "#8a8f98",
        fontFamily: "Inter, Segoe UI, sans-serif",
        fontSize: 12,
      },
      top: 0,
    },
    tooltip: {
      trigger: "axis",
      backgroundColor: "#18191a",
      borderColor: "#23252a",
      borderWidth: 1,
      padding: 12,
      textStyle: {
        color: "#f7f8f8",
        fontFamily: "Inter, Segoe UI, sans-serif",
      },
      valueFormatter: (value: unknown) =>
        typeof value === "number" ? value.toLocaleString() : `${value}`,
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: points.map((point) => {
        try {
          // Parse ISO string and format as local time (e.g. 15:00)
          return new Date(point.time).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
        } catch {
          return point.time;
        }
      }),
      axisLine: {
        lineStyle: {
          color: "#23252a",
        },
      },
      axisLabel: {
        color: "#8a8f98",
        fontFamily: "Inter, Segoe UI, sans-serif",
      },
      axisTick: {
        show: false,
      },
    },
    yAxis: {
      type: "value",
      splitLine: {
        lineStyle: {
          color: "#23252a",
          type: "dashed",
        },
      },
      axisLabel: {
        color: "#8a8f98",
        fontFamily: "Inter, Segoe UI, sans-serif",
      },
    },
    series: levels.map((level) => ({
      name: level,
      type: "line",
      stack: "logs",
      smooth: true,
      symbol: "none",
      areaStyle: {
        opacity: level === "INFO" ? 0.22 : 0.34,
      },
      emphasis: {
        focus: "series",
      },
      lineStyle: {
        width: level === "CRITICAL" ? 2.5 : 2,
      },
      data: points.map((point) => point[level]),
    })),
  };

  return (
    <section
      aria-label="Log volume by level"
      className="rounded-lg border border-border bg-surface p-5"
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-text">Log volume</h2>
          <p className="mt-1 text-sm text-muted">
            Throughput and severity mix for authorized applications.
          </p>
        </div>
        <div className="flex flex-wrap gap-3 text-xs text-muted">
          {levels.map((level) => (
            <span className="inline-flex items-center gap-1.5" key={level}>
              <span
                className={`h-2.5 w-2.5 rounded-sm ${levelClasses[level]}`}
              />
              {level}
            </span>
          ))}
        </div>
      </div>

      {points.length === 0 ? (
        <p className="mt-5 rounded-lg border border-border bg-surface-raised p-4 text-sm text-muted">
          No log volume data in the selected window.
        </p>
      ) : (
        <div className="mt-6 rounded-lg border border-border bg-background/60 p-3">
          <ReactECharts
            option={option}
            opts={{ renderer: "canvas" }}
            style={{ height: 320, width: "100%" }}
          />
        </div>
      )}

      <div className="mt-5 grid gap-3 sm:grid-cols-4">
        {levels.map((level) => {
          const total = points.reduce((sum, point) => sum + point[level], 0);
          return (
            <div
              className="rounded-lg border border-border bg-surface-raised p-3"
              key={level}
            >
              <div className="flex items-center gap-2">
                <span
                  className={`h-2.5 w-2.5 rounded-sm ${levelClasses[level]}`}
                />
                <span className="text-xs font-semibold uppercase text-muted">
                  {level}
                </span>
              </div>
              <p className="mt-2 text-lg font-semibold text-text">
                {total.toLocaleString()}
              </p>
            </div>
          );
        })}
      </div>
    </section>
  );
}
