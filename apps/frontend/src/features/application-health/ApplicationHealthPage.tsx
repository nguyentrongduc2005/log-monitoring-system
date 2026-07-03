import ReactECharts from "echarts-for-react";
import { useEffect, useMemo, useState } from "react";
import { getApplicationHealthSnapshot } from "./application-health-adapter";
import type {
  ApplicationErrorRatePoint,
  ApplicationHealthSnapshot,
  ApplicationStability,
  HealthMetric,
  HealthStatus,
  HealthWindow,
} from "./application-health-types";
import { PageHeader } from "@/shared/layouts/page-header-context";

const windows: HealthWindow[] = ["15m", "1h", "6h", "24h"];

const metricClasses: Record<HealthMetric["tone"], string> = {
  neutral: "border-border bg-surface-raised text-text",
  success: "border-success/30 bg-success/10 text-success",
  warning: "border-warning/30 bg-warning/10 text-warning",
  error: "border-error/30 bg-error/10 text-error",
};

const statusClasses: Record<Exclude<HealthStatus, "NO_DATA">, string> = {
  HEALTHY: "border-success/30 bg-success/10 text-success",
  DEGRADED: "border-warning/30 bg-warning/10 text-warning",
  CRITICAL: "border-error/30 bg-error/10 text-error",
};

function createEmptySnapshot(): ApplicationHealthSnapshot {
  return {
    application: { id: "checkout-api", name: "Checkout API" },
    applications: [],
    window: "Last 15 minutes",
    generatedAt: "",
    comparisonSummary: [],
    hourlyErrorRates: [],
    stabilityRanking: [],
    healthScore: 0,
    healthStatus: "NO_DATA",
    healthReason: "",
    metrics: [],
    trend: [],
    severityBreakdown: [],
    topFingerprints: [],
    recentAlerts: [],
    problemLogs: [],
  };
}

export function Component() {
  const [selectedWindow, setSelectedWindow] = useState<HealthWindow>("15m");
  const [selectedApplicationId, setSelectedApplicationId] = useState<
    string | null
  >(null);
  const [snapshot, setSnapshot] =
    useState<ApplicationHealthSnapshot>(createEmptySnapshot);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  async function loadSnapshot() {
    setLoading(true);
    setError(null);
    try {
      const nextSnapshot = await getApplicationHealthSnapshot(
        undefined,
        selectedWindow,
      );
      setSnapshot(nextSnapshot);
    } catch {
      setError("Unable to load application health analytics right now.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => {
      void loadSnapshot();
    });
  }, [selectedWindow]);

  const comparisonOption = useMemo(
    () => ({
      backgroundColor: "transparent",
      color: ["#ef4444", "#f59e0b", "#5e6ad2", "#27a644"],
      grid: { left: 42, right: 24, top: 34, bottom: 34 },
      legend: {
        data: snapshot.applications.map((application) => application.name),
        icon: "circle",
        itemHeight: 9,
        itemWidth: 9,
        right: 0,
        textStyle: { color: "#8a8f98", fontSize: 12 },
        top: 0,
      },
      tooltip: {
        trigger: "axis",
        backgroundColor: "#18191a",
        borderColor: "#23252a",
        textStyle: { color: "#f7f8f8" },
        valueFormatter: (value: unknown) =>
          typeof value === "number" ? `${value.toFixed(1)}%` : `${value}`,
      },
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: snapshot.hourlyErrorRates.map((point) => point.time),
        axisLine: { lineStyle: { color: "#23252a" } },
        axisLabel: { color: "#8a8f98" },
        axisTick: { show: false },
      },
      yAxis: {
        type: "value",
        name: "Error rate %",
        nameTextStyle: { color: "#8a8f98" },
        splitLine: { lineStyle: { color: "#23252a", type: "dashed" } },
        axisLabel: { color: "#8a8f98", formatter: "{value}%" },
      },
      series: snapshot.applications.map((application) => ({
        name: application.name,
        type: "line",
        smooth: true,
        symbolSize: 7,
        lineStyle: { width: 3 },
        emphasis: { focus: "series" },
        data: snapshot.hourlyErrorRates.map(
          (point) => point.rates[application.id] ?? 0,
        ),
      })),
    }),
    [snapshot.applications, snapshot.hourlyErrorRates],
  );

  const selectedApplication =
    snapshot.stabilityRanking.find(
      (application) => application.applicationId === selectedApplicationId,
    ) ??
    snapshot.stabilityRanking[0] ??
    null;

  return (
    <div className="space-y-6">
      <PageHeader
        actions={
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
        }
        title="Application Health"
      />

      {loading ? (
        <section className="rounded-lg border border-border bg-surface p-5 text-sm text-muted">
          Loading application health analytics...
        </section>
      ) : null}

      {error ? (
        <section className="rounded-lg border border-border bg-surface p-5">
          <p className="text-sm text-error">{error}</p>
        </section>
      ) : null}

      {!error ? (
        <>
          <section className="rounded-lg border border-border bg-surface p-5">
            <div className="flex flex-wrap items-start justify-between gap-4">
              <div>
                <p className="text-sm text-muted">
                  Hourly error-rate comparison across applications ·{" "}
                  {snapshot.window}
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
            {snapshot.comparisonSummary.map((metric) => (
              <MetricCard key={metric.id} metric={metric} />
            ))}
          </section>

          <article
            aria-label="Hourly error rate by application"
            className="rounded-lg border border-border bg-surface p-5"
          >
            <h2 className="text-lg font-semibold text-text">
              Hourly error rate by application
            </h2>
            <p className="mt-1 text-sm text-muted">
              Compare ERROR and CRITICAL share by application to identify the
              least stable service.
            </p>
            <div className="mt-5 rounded-lg border border-border bg-background/60 p-3">
              <ReactECharts
                option={comparisonOption}
                opts={{ renderer: "canvas" }}
                style={{ height: 400, width: "100%" }}
              />
            </div>
          </article>

          <StabilityRanking
            onSelectApplication={setSelectedApplicationId}
            ranking={snapshot.stabilityRanking}
            selectedApplicationId={
              selectedApplication?.applicationId ?? selectedApplicationId
            }
          />

          {selectedApplication ? (
            <ApplicationDetailCard
              application={selectedApplication}
              hourlyErrorRates={snapshot.hourlyErrorRates}
            />
          ) : null}
        </>
      ) : null}
    </div>
  );
}

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
          <linearGradient id={`grad-health-${tone}`} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={activeColor.stop} stopOpacity="0.25" />
            <stop offset="100%" stopColor={activeColor.stop} stopOpacity="0.0" />
          </linearGradient>
        </defs>
        <path
          d={paths[tone]}
          fill={`url(#grad-health-${tone})`}
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

function MetricCard({ metric }: { metric: HealthMetric }) {
  const isNumeric = typeof metric.value === "number" || (typeof metric.value === "string" && !isNaN(Number(metric.value.replace(/[,%]/g, ""))));

  return (
    <article className={`rounded-lg border p-4 flex flex-col justify-between min-h-[110px] ${metricClasses[metric.tone]}`}>
      <div className="w-full">
        <p className="text-[10px] font-semibold uppercase tracking-wider text-muted/70">
          {metric.label}
        </p>
        <p className="mt-2.5 text-2xl font-bold leading-none tracking-tight">{metric.value}</p>
      </div>
      <div className="mt-3 flex items-center justify-between gap-2 w-full">
        {metric.helper ? (
          <p className="text-[11px] text-muted/80 truncate flex-1">{metric.helper}</p>
        ) : <div className="flex-1" />}
        {isNumeric ? <Sparkline tone={metric.tone} /> : null}
      </div>
    </article>
  );
}

function StabilityRanking({
  onSelectApplication,
  ranking,
  selectedApplicationId,
}: {
  onSelectApplication: (applicationId: string) => void;
  ranking: ApplicationStability[];
  selectedApplicationId: string | null;
}) {
  return (
    <article className="rounded-lg border border-border bg-surface p-5">
      <h2 className="text-lg font-semibold text-text">
        Least stable applications
      </h2>
      <p className="mt-1 text-sm text-muted">
        Ranked by average and peak error rate. Select a row to inspect details.
      </p>
      <div className="mt-5 overflow-x-auto">
        <table className="min-w-full text-left text-sm">
          <thead className="text-xs uppercase text-muted">
            <tr>
              <th className="pb-3">Rank</th>
              <th className="pb-3">Application</th>
              <th className="pb-3">Avg error</th>
              <th className="pb-3">Peak error</th>
              <th className="pb-3">Critical logs</th>
              <th className="pb-3">Open alerts</th>
              <th className="pb-3">Status</th>
            </tr>
          </thead>
          <tbody>
            {ranking.map((application, index) => {
              const selected =
                selectedApplicationId === application.applicationId;

              return (
                <tr
                  aria-current={selected ? "true" : undefined}
                  className={`cursor-pointer border-t border-border transition hover:bg-surface-raised/70 ${
                    selected ? "bg-primary/10" : ""
                  }`}
                  key={application.applicationId}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                      event.preventDefault();
                      onSelectApplication(application.applicationId);
                    }
                  }}
                  onClick={() => onSelectApplication(application.applicationId)}
                  role="button"
                  tabIndex={0}
                >
                  <td className="py-3 text-muted">{index + 1}</td>
                  <td className="py-3 font-medium text-text">
                    {application.applicationName}
                  </td>
                  <td className="py-3 text-text">
                    {application.avgErrorRate.toFixed(1)}%
                  </td>
                  <td className="py-3 text-warning">
                    {application.peakErrorRate.toFixed(1)}%
                  </td>
                  <td className="py-3 text-error">
                    {application.criticalLogs}
                  </td>
                  <td className="py-3 text-text">{application.openAlerts}</td>
                  <td className="py-3">
                    <span
                      className={`rounded-full border px-2 py-0.5 text-[11px] font-semibold ${statusClasses[application.status]}`}
                    >
                      {application.status}
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </article>
  );
}

function ApplicationDetailCard({
  application,
  hourlyErrorRates,
}: {
  application: ApplicationStability;
  hourlyErrorRates: ApplicationErrorRatePoint[];
}) {
  const appTrend = hourlyErrorRates.map((point) => ({
    time: point.time,
    errorRate: point.rates[application.applicationId] ?? 0,
  }));
  const latestPoint = appTrend.at(-1);
  const firstPoint = appTrend[0];
  const trendDelta =
    latestPoint && firstPoint
      ? latestPoint.errorRate - firstPoint.errorRate
      : 0;
  const trendLabel =
    trendDelta > 0.5
      ? `Up ${trendDelta.toFixed(1)} pts`
      : trendDelta < -0.5
        ? `Down ${Math.abs(trendDelta).toFixed(1)} pts`
        : "Stable";
  const trendTone =
    trendDelta > 0.5 ? "error" : trendDelta < -0.5 ? "success" : "neutral";
  const trendOption = {
    backgroundColor: "transparent",
    color: ["#ef4444"],
    grid: { left: 36, right: 18, top: 18, bottom: 28 },
    tooltip: {
      trigger: "axis",
      backgroundColor: "#18191a",
      borderColor: "#23252a",
      textStyle: { color: "#f7f8f8" },
      valueFormatter: (value: unknown) =>
        typeof value === "number" ? `${value.toFixed(1)}%` : `${value}`,
    },
    xAxis: {
      type: "category",
      boundaryGap: false,
      data: appTrend.map((point) => point.time),
      axisLine: { lineStyle: { color: "#23252a" } },
      axisLabel: { color: "#8a8f98" },
      axisTick: { show: false },
    },
    yAxis: {
      type: "value",
      splitLine: { lineStyle: { color: "#23252a", type: "dashed" } },
      axisLabel: { color: "#8a8f98", formatter: "{value}%" },
    },
    series: [
      {
        name: "Error rate",
        type: "line",
        smooth: true,
        symbolSize: 7,
        lineStyle: { width: 3 },
        areaStyle: { opacity: 0.18 },
        markPoint: {
          data: [{ type: "max", name: "Peak" }],
          label: { color: "#010102", fontWeight: 700 },
        },
        data: appTrend.map((point) => point.errorRate),
      },
    ],
  };

  return (
    <section className="rounded-lg border border-border bg-surface p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-text">
            Selected application analysis
          </h2>
          <p className="mt-1 text-sm text-muted">
            Focused stability breakdown for the selected application.
          </p>
        </div>
        <span
          className={`rounded-full border px-3 py-1 text-xs font-semibold ${
            trendTone === "error"
              ? "border-error/30 bg-error/10 text-error"
              : trendTone === "success"
                ? "border-success/30 bg-success/10 text-success"
                : "border-border bg-surface-raised text-muted"
          }`}
        >
          {trendLabel}
        </span>
      </div>

      <article className="mt-5 rounded-lg border border-border bg-surface-raised p-5">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-xl font-semibold text-text">
            {application.applicationName}
          </h3>
          <span
            className={`rounded-full border px-3 py-1 text-xs font-semibold ${statusClasses[application.status]}`}
          >
            {application.status}
          </span>
        </div>

        <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1.25fr)_minmax(22rem,0.75fr)]">
          <div className="rounded-lg border border-border bg-background p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h4 className="text-sm font-semibold text-text">
                  Hourly error-rate trend
                </h4>
                <p className="mt-1 text-xs text-muted">
                  Selected app only, with peak point marked.
                </p>
              </div>
              <span className="text-sm font-semibold text-warning">
                Peak {application.peakErrorRate.toFixed(1)}%
              </span>
            </div>
            <div className="mt-4">
              <ReactECharts
                option={trendOption}
                opts={{ renderer: "canvas" }}
                style={{ height: 260, width: "100%" }}
              />
            </div>
          </div>

          <div className="rounded-lg border border-border bg-background p-4">
            <h4 className="text-sm font-semibold text-text">
              Stability verdict
            </h4>
            <p className="mt-3 text-sm text-text">
              {application.detail.insight}
            </p>
            <dl className="mt-4 grid gap-3 text-sm">
              <CompactStat
                label="Top issue"
                value={application.detail.topIssue}
              />
              <CompactStat
                label="Peak hour"
                value={application.detail.peakHour}
              />
              <CompactStat
                label="Last evaluated"
                value={application.detail.lastEvaluated}
              />
            </dl>
          </div>
        </div>

        <dl className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-7">
          <DetailStat
            label="Total logs"
            value={application.detail.totalLogs.toLocaleString()}
          />
          <DetailStat
            label="Error logs"
            tone="error"
            value={application.detail.errorLogs.toLocaleString()}
          />
          <DetailStat
            label="Warning logs"
            tone="warning"
            value={application.detail.warningLogs.toLocaleString()}
          />
          <DetailStat
            label="Average error rate"
            value={`${application.avgErrorRate.toFixed(1)}%`}
          />
          <DetailStat
            label="Peak error rate"
            tone="warning"
            value={`${application.peakErrorRate.toFixed(1)}%`}
          />
          <DetailStat
            label="Critical logs"
            tone="error"
            value={String(application.criticalLogs)}
          />
          <DetailStat
            label="Open alerts"
            value={String(application.openAlerts)}
          />
        </dl>
      </article>
    </section>
  );
}

function DetailStat({
  label,
  tone = "neutral",
  value,
}: {
  label: string;
  tone?: "neutral" | "warning" | "error";
  value: string;
}) {
  return (
    <div className="rounded-lg border border-border bg-background p-4">
      <dt className="text-xs font-semibold uppercase text-muted">{label}</dt>
      <dd
        className={`mt-2 text-2xl font-semibold ${
          tone === "error"
            ? "text-error"
            : tone === "warning"
              ? "text-warning"
              : "text-text"
        }`}
      >
        {value}
      </dd>
    </div>
  );
}

function CompactStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border bg-surface-raised p-3">
      <dt className="text-xs font-semibold uppercase text-muted">{label}</dt>
      <dd className="mt-1 break-words text-sm font-medium text-text">
        {value}
      </dd>
    </div>
  );
}
