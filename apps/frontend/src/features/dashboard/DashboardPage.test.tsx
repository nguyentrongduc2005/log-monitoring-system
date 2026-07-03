import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Component as DashboardPage } from "@/features/dashboard/DashboardPage";
import { getOverviewSnapshot } from "@/features/dashboard/overview-adapter";
import type { OverviewSnapshot } from "@/features/dashboard/overview-types";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";

vi.mock("@/features/dashboard/overview-adapter", () => ({
  getOverviewSnapshot: vi.fn(),
}));

vi.mock("echarts-for-react", () => ({
  default: () => <div aria-label="Rendered log volume chart" />,
}));

const baseSnapshot: OverviewSnapshot = {
  window: "Last 15 minutes",
  generatedAt: "2026-06-23T10:30:00Z",
  metrics: [
    {
      id: "logs-per-second",
      label: "Log tiếp nhận/s",
      value: "1,228",
      tone: "success",
    },
    { id: "error-rate", label: "Error rate", value: "2.4%", tone: "warning" },
    {
      id: "critical-alerts",
      label: "Open critical",
      value: "3",
      tone: "error",
    },
    {
      id: "active-apps",
      label: "Active applications",
      value: "12",
      tone: "neutral",
    },
    {
      id: "lag",
      label: "Processing lag",
      value: "380 ms",
      tone: "success",
    },
  ],
  volume: [
    { time: "09:55", INFO: 20, WARN: 5, ERROR: 2, CRITICAL: 1 },
    { time: "10:00", INFO: 24, WARN: 6, ERROR: 3, CRITICAL: 1 },
  ],
  criticalAlerts: [
    {
      id: "critical-1",
      severity: "CRITICAL",
      application: "checkout-api",
      logSamples: [{ level: "CRITICAL", message: "Payment gateway timeout crossed alert threshold." }],
      occurrences: 28,
      lastSeen: "10:15 UTC",
      deliveryState: "Delivered",
    },
  ],
};

function renderDashboardPage() {
  render(
    <PageHeaderProvider>
      <DashboardPage />
    </PageHeaderProvider>,
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
    expect(await screen.findByText("Dashboard")).toBeInTheDocument();
    expect(screen.getByText("Operations overview")).toBeInTheDocument();
    expect(screen.getByText("Log tiếp nhận/s")).toBeInTheDocument();
    expect(screen.getAllByText("Error rate")).not.toHaveLength(0);
    expect(screen.getByText("Open critical")).toBeInTheDocument();
    expect(screen.getByText("Active applications")).toBeInTheDocument();
    expect(screen.getByText("Processing lag")).toBeInTheDocument();
    expect(screen.getByLabelText("Log volume by level")).toBeInTheDocument();
    expect(
      screen.getByText("Payment gateway timeout crossed alert threshold."),
    ).toBeInTheDocument();
  });

  it("renders retryable error state when the adapter fails", async () => {
    vi.mocked(getOverviewSnapshot).mockRejectedValueOnce(new Error("boom"));
    renderDashboardPage();

    expect(
      await screen.findByText(
        "Unable to load the overview snapshot right now. Please try again.",
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });

  it("renders useful empty states without removing KPI and pipeline sections", async () => {
    vi.mocked(getOverviewSnapshot).mockResolvedValueOnce({
      ...baseSnapshot,
      criticalAlerts: [],
    });

    renderDashboardPage();

    expect(await screen.findByText("Log tiếp nhận/s")).toBeInTheDocument();
    expect(
      screen.getByText("No critical alerts in the current window."),
    ).toBeInTheDocument();
  });



  it("reloads adapter data when Refresh is clicked", async () => {
    const user = userEvent.setup();
    renderDashboardPage();

    await screen.findByText("Log tiếp nhận/s");
    await user.click(screen.getByRole("button", { name: "Refresh" }));

    await waitFor(() => expect(getOverviewSnapshot).toHaveBeenCalledTimes(2));
  });

  it("reloads chart data for the selected time window", async () => {
    const user = userEvent.setup();
    renderDashboardPage();

    await screen.findByText("Log tiếp nhận/s");
    await user.click(screen.getByRole("button", { name: "1h" }));

    await waitFor(() =>
      expect(getOverviewSnapshot).toHaveBeenLastCalledWith("1h"),
    );
  });
});
