import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Component as DashboardPage } from "@/features/dashboard/DashboardPage";
import { getOverviewSnapshot } from "@/features/dashboard/overview-adapter";
import type { OverviewSnapshot } from "@/features/dashboard/overview-types";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";

vi.mock("@/features/dashboard/overview-adapter", () => ({
  getOverviewSnapshot: vi.fn()
}));

const baseSnapshot: OverviewSnapshot = {
  metrics: [
    {
      id: "accepted",
      label: "Logs accepted/min",
      value: "14,820",
      tone: "success"
    },
    { id: "error-rate", label: "Error rate", value: "2.4%", tone: "warning" },
    {
      id: "critical-alerts",
      label: "Critical alerts",
      value: "3",
      tone: "error"
    },
    {
      id: "active-apps",
      label: "Active applications",
      value: "12",
      tone: "neutral"
    },
    {
      id: "lag",
      label: "Processing lag",
      value: "380 ms",
      tone: "success"
    }
  ],
  pipeline: [
    { id: "1", label: "Ingestion API", state: "healthy", detail: "ok" },
    { id: "2", label: "Kafka logs.raw", state: "healthy", detail: "ok" },
    { id: "3", label: "Worker", state: "degraded", detail: "lag" },
    { id: "4", label: "ClickHouse", state: "offline", detail: "paused" },
    { id: "5", label: "WebSocket", state: "unknown", detail: "unknown" },
    {
      id: "6",
      label: "Alerting / Telegram",
      state: "healthy",
      detail: "ok"
    }
  ],
  volume: [
    { time: "09:55", INFO: 20, WARN: 5, ERROR: 2, CRITICAL: 1 },
    { time: "10:00", INFO: 24, WARN: 6, ERROR: 3, CRITICAL: 1 }
  ],
  noisyApplications: [
    {
      id: "checkout-api",
      name: "checkout-api",
      environment: "production",
      totalLogs: 6240,
      errorCount: 132,
      criticalCount: 8,
      errorRate: "2.2%",
      lastSeen: "10:15 UTC"
    }
  ],
  criticalAlerts: [
    {
      id: "critical-1",
      severity: "CRITICAL",
      application: "checkout-api",
      fingerprint: "PAYMENT_GATEWAY_TIMEOUT",
      message: "Payment gateway timeout crossed alert threshold.",
      occurrences: 28,
      lastSeen: "10:15 UTC",
      deliveryState: "Delivered"
    }
  ],
  demoReadiness: {
    accepted: 500,
    rejected: 0,
    duration: "2.0s",
    p95AckLatency: "84 ms",
    streamStatus: "Smooth",
    buffered: 8,
    dropped: 0
  },
  authorizedApplications: 12
};

function renderDashboardPage() {
  render(
    <PageHeaderProvider>
      <DashboardPage />
    </PageHeaderProvider>
  );
}

describe("DashboardPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getOverviewSnapshot).mockResolvedValue(baseSnapshot);
  });

  it("renders overview sections from adapter data", async () => {
    renderDashboardPage();

    expect(screen.getByText("Loading overview...")).toBeInTheDocument();
    expect(await screen.findByText("Logs accepted/min")).toBeInTheDocument();
    expect(screen.getAllByText("Error rate")).not.toHaveLength(0);
    expect(screen.getByText("Critical alerts")).toBeInTheDocument();
    expect(screen.getByText("Active applications")).toBeInTheDocument();
    expect(screen.getByText("Processing lag")).toBeInTheDocument();
    expect(screen.getByText("Ingestion API")).toBeInTheDocument();
    expect(screen.getByText("Kafka logs.raw")).toBeInTheDocument();
    expect(screen.getByText("Worker")).toBeInTheDocument();
    expect(screen.getByText("ClickHouse")).toBeInTheDocument();
    expect(screen.getByText("WebSocket")).toBeInTheDocument();
    expect(screen.getByText("Alerting / Telegram")).toBeInTheDocument();
    expect(
      screen.getByLabelText("Log volume by level")
    ).toBeInTheDocument();
    expect(screen.getAllByText("checkout-api")).not.toHaveLength(0);
    expect(
      screen.getByText("Payment gateway timeout crossed alert threshold.")
    ).toBeInTheDocument();
    expect(screen.getByText("500 logs / 2 seconds")).toBeInTheDocument();
  });

  it("renders retryable error state when the adapter fails", async () => {
    vi.mocked(getOverviewSnapshot).mockRejectedValueOnce(new Error("boom"));
    renderDashboardPage();

    expect(
      await screen.findByText(
        "Unable to load the overview snapshot right now. Please try again."
      )
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });

  it("renders useful empty states without removing KPI and pipeline sections", async () => {
    vi.mocked(getOverviewSnapshot).mockResolvedValueOnce({
      ...baseSnapshot,
      noisyApplications: [],
      criticalAlerts: []
    });

    renderDashboardPage();

    expect(await screen.findByText("Logs accepted/min")).toBeInTheDocument();
    expect(screen.getByText("Pipeline flow")).toBeInTheDocument();
    expect(screen.getByText("No noisy applications in the current window.")).toBeInTheDocument();
    expect(screen.getByText("No critical alerts in the current window.")).toBeInTheDocument();
  });

  it("renders the no-authorized-applications state", async () => {
    vi.mocked(getOverviewSnapshot).mockResolvedValueOnce({
      ...baseSnapshot,
      authorizedApplications: 0,
      noisyApplications: []
    });

    renderDashboardPage();

    expect(
      await screen.findByText(
        "No authorized applications are available for this account yet."
      )
    ).toBeInTheDocument();
  });

  it("renders degraded, offline, and unknown pipeline states", async () => {
    renderDashboardPage();

    expect(await screen.findByText("degraded")).toBeInTheDocument();
    expect(screen.getByText("offline")).toBeInTheDocument();
    expect(screen.getAllByText("unknown")).not.toHaveLength(0);
  });

  it("reloads adapter data when Refresh is clicked", async () => {
    const user = userEvent.setup();
    renderDashboardPage();

    await screen.findByText("Logs accepted/min");
    await user.click(screen.getByRole("button", { name: "Refresh" }));

    await waitFor(() => expect(getOverviewSnapshot).toHaveBeenCalledTimes(2));
  });
});
