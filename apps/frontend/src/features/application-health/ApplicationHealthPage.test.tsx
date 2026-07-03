import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import { Component as ApplicationHealthPage } from "./ApplicationHealthPage";
import { getApplicationHealthSnapshot } from "./application-health-adapter";
import type { ApplicationHealthSnapshot } from "./application-health-types";

vi.mock("echarts-for-react", () => ({
  default: () => <div aria-label="Rendered application health chart" />,
}));

vi.mock("./application-health-adapter", () => ({
  getApplicationHealthSnapshot: vi.fn(),
}));

const snapshot: ApplicationHealthSnapshot = {
  application: { id: "checkout-api", name: "Checkout API" },
  applications: [
    { id: "checkout-api", name: "Checkout API" },
    { id: "billing-worker", name: "Billing Worker" },
  ],
  window: "Last 15 minutes",
  generatedAt: "2026-06-23T10:30:00Z",
  comparisonSummary: [
    {
      id: "tracked-apps",
      label: "Tracked apps",
      value: "2",
      tone: "neutral",
    },
    {
      id: "worst-app",
      label: "Least stable",
      value: "Checkout API",
      tone: "error",
    },
  ],
  hourlyErrorRates: [
    {
      time: "10:15",
      rates: { "checkout-api": 3.4, "billing-worker": 2.8 },
    },
    {
      time: "10:30",
      rates: { "checkout-api": 5.9, "billing-worker": 4.2 },
    },
  ],
  stabilityRanking: [
    {
      applicationId: "checkout-api",
      applicationName: "Checkout API",
      avgErrorRate: 4.5,
      peakErrorRate: 5.9,
      criticalLogs: 8,
      openAlerts: 4,
      status: "DEGRADED",
      detail: {
        totalLogs: 5200,
        errorLogs: 310,
        warningLogs: 322,
        peakHour: "10:30",
        lastEvaluated: "10:30 UTC",
        topIssue: "PAYMENT_GATEWAY_TIMEOUT",
        insight: "Payment timeouts are driving the current error-rate spike.",
      },
    },
    {
      applicationId: "billing-worker",
      applicationName: "Billing Worker",
      avgErrorRate: 3.1,
      peakErrorRate: 4.2,
      criticalLogs: 6,
      openAlerts: 2,
      status: "DEGRADED",
      detail: {
        totalLogs: 4512,
        errorLogs: 188,
        warningLogs: 418,
        peakHour: "10:30",
        lastEvaluated: "10:30 UTC",
        topIssue: "INVOICE_RETRY_STORM",
        insight: "Invoice retries are elevated but lower than Checkout API.",
      },
    },
  ],
  healthScore: 62,
  healthStatus: "DEGRADED",
  healthReason: "Payment timeout fingerprint is increasing.",
  metrics: [
    { id: "total-logs", label: "Total logs", value: "5,200", tone: "neutral" },
    { id: "error-rate", label: "Error rate", value: "5.9%", tone: "warning" },
    { id: "open-alerts", label: "Open alerts", value: "4", tone: "error" },
  ],
  trend: [
    { time: "10:15", totalLogs: 1085, errorRate: 3.4, criticalAlerts: 1 },
    { time: "10:30", totalLogs: 1398, errorRate: 5.9, criticalAlerts: 4 },
  ],
  severityBreakdown: [
    { level: "INFO", count: 4560, percentage: 88 },
    { level: "WARN", count: 322, percentage: 6 },
    { level: "ERROR", count: 310, percentage: 5 },
    { level: "CRITICAL", count: 8, percentage: 1 },
  ],
  topFingerprints: [
    {
      id: "fp-payment-timeout",
      fingerprint: "PAYMENT_GATEWAY_TIMEOUT",
      message: "Payment gateway timeout after 3000ms.",
      severity: "CRITICAL",
      occurrences: 28,
      firstSeen: "10:03",
      lastSeen: "10:29",
      trend: "Increasing",
    },
  ],
  recentAlerts: [
    {
      id: "alert-payment-timeout",
      severity: "CRITICAL",
      ruleName: "Payment failures",
      logSamples: [{ level: "CRITICAL", message: "Payment gateway timeout crossed alert threshold." }],
      occurrences: 28,
      status: "OPEN",
      lastSeen: "10:29",
    },
  ],
  problemLogs: [
    {
      id: "log-1",
      timestamp: "10:29:42",
      level: "CRITICAL",
      fingerprint: "PAYMENT_GATEWAY_TIMEOUT",
      message: "Payment gateway timeout after 3000ms for order 90321.",
      traceId: "trace-7f21c",
    },
  ],
};

function renderPage() {
  render(
    <PageHeaderProvider>
      <ApplicationHealthPage />
    </PageHeaderProvider>,
  );
}

describe("ApplicationHealthPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getApplicationHealthSnapshot).mockResolvedValue(snapshot);
  });

  it("renders application health analytics from the adapter", async () => {
    renderPage();

    expect(await screen.findAllByText("Checkout API")).not.toHaveLength(0);
    expect(
      screen.getByText("Application Health"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Hourly error rate by application"),
    ).toBeInTheDocument();
    expect(screen.getByText("Least stable applications")).toBeInTheDocument();
    expect(screen.getByText("Tracked apps")).toBeInTheDocument();
    expect(
      screen.getByText("Selected application analysis"),
    ).toBeInTheDocument();
    expect(screen.getByText("Hourly error-rate trend")).toBeInTheDocument();
    expect(screen.getByText("Stability verdict")).toBeInTheDocument();
    expect(screen.getAllByText("Peak error rate")).not.toHaveLength(0);
    expect(screen.getByText("Top issue")).toBeInTheDocument();
    expect(screen.getByText("PAYMENT_GATEWAY_TIMEOUT")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Payment timeouts are driving the current error-rate spike.",
      ),
    ).toBeInTheDocument();
    expect(screen.getAllByText("5.9%")).not.toHaveLength(0);
    expect(
      screen.queryByText("Hourly comparison detail"),
    ).not.toBeInTheDocument();
    expect(screen.queryByText("Signal")).not.toBeInTheDocument();
    expect(screen.queryByText("Highest error rate")).not.toBeInTheDocument();
    expect(screen.queryByText("Health score")).not.toBeInTheDocument();
    expect(screen.queryByText("Recent problem logs")).not.toBeInTheDocument();
  });

  it("loads another time window when selected", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findAllByText("Checkout API");
    await user.click(screen.getByRole("button", { name: "1h" }));

    await waitFor(() =>
      expect(getApplicationHealthSnapshot).toHaveBeenLastCalledWith(
        undefined,
        "1h",
      ),
    );
  });

  it("shows detail for the selected application", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findAllByText("Checkout API");
    await user.click(screen.getByRole("button", { name: /Billing Worker/ }));

    expect(screen.getAllByText("4.2%")).not.toHaveLength(0);
    expect(screen.getByText("INVOICE_RETRY_STORM")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Invoice retries are elevated but lower than Checkout API.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByText("Hourly error-rate trend")).toBeInTheDocument();
  });
});
