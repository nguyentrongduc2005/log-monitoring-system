import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { Component as LiveLogsPage } from "@/features/live-logs/LiveLogsPage";
import {
  getInitialLiveLogSnapshot,
  MAX_VISIBLE_LOGS
} from "@/features/live-logs/live-logs-adapter";
import type { LiveLogSnapshot } from "@/features/live-logs/live-logs-types";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";

vi.mock("@/features/live-logs/live-logs-adapter", async () => {
  const actual = await vi.importActual<
    typeof import("@/features/live-logs/live-logs-adapter")
  >("@/features/live-logs/live-logs-adapter");

  return {
    ...actual,
    getInitialLiveLogSnapshot: vi.fn()
  };
});

const baseSnapshot: LiveLogSnapshot = {
  applications: [
    { id: "checkout-api", name: "checkout-api" },
    { id: "billing-worker", name: "billing-worker" }
  ],
  entries: [
    {
      id: "log-1",
      timestamp: "2026-06-09T10:15:04Z",
      applicationId: "checkout-api",
      applicationName: "checkout-api",
      level: "CRITICAL",
      message: "Payment gateway timeout exceeded the critical threshold.",
      traceId: "trace-checkout-1",
      eventId: "evt-1001",
      ingestionId: "ing-9001",
      source: "api",
      host: "checkout-01",
      attributes: { region: "ap-southeast-1" }
    },
    {
      id: "log-2",
      timestamp: "2026-06-09T10:15:01Z",
      applicationId: "billing-worker",
      applicationName: "billing-worker",
      level: "ERROR",
      message: "Invoice retry queue is growing faster than the worker drain rate.",
      traceId: "trace-billing-1",
      eventId: "evt-1002",
      ingestionId: "ing-9002",
      source: "worker",
      host: "billing-02"
    },
    {
      id: "log-3",
      timestamp: "2026-06-09T10:14:50Z",
      applicationId: "checkout-api",
      applicationName: "checkout-api",
      level: "INFO",
      message: "Order checkout completed successfully.",
      traceId: "trace-checkout-2",
      eventId: "evt-1003",
      ingestionId: "ing-9003",
      source: "api",
      host: "checkout-01"
    }
  ],
  connectionState: "live",
  buffered: 2,
  dropped: 1
};

function renderLiveLogsPage() {
  render(
    <PageHeaderProvider>
      <LiveLogsPage />
    </PageHeaderProvider>
  );
}

describe("LiveLogsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getInitialLiveLogSnapshot).mockResolvedValue(baseSnapshot);
  });

  it("renders live logs page and connection status", async () => {
    renderLiveLogsPage();

    expect(screen.getByText("Loading live logs...")).toBeInTheDocument();
    expect(await screen.findByText("Payment gateway timeout exceeded the critical threshold.")).toBeInTheDocument();
    expect(screen.getByText("Live")).toBeInTheDocument();
  });

  it("filters by application, level, keyword, and trace ID", async () => {
    const user = userEvent.setup();
    renderLiveLogsPage();

    await screen.findByText("Payment gateway timeout exceeded the critical threshold.");
    await user.selectOptions(screen.getByLabelText("Application"), "billing-worker");
    expect(screen.getByText("Invoice retry queue is growing faster than the worker drain rate.")).toBeInTheDocument();
    expect(screen.queryByText("Payment gateway timeout exceeded the critical threshold.")).not.toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Application"), "");
    await user.selectOptions(screen.getByLabelText("Level"), "CRITICAL");
    expect(screen.getByText("Payment gateway timeout exceeded the critical threshold.")).toBeInTheDocument();
    expect(screen.queryByText("Invoice retry queue is growing faster than the worker drain rate.")).not.toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Level"), "ALL");
    await user.type(screen.getByLabelText("Keyword"), "checkout completed");
    expect(screen.getByText("Order checkout completed successfully.")).toBeInTheDocument();
    expect(screen.queryByText("Invoice retry queue is growing faster than the worker drain rate.")).not.toBeInTheDocument();

    await user.clear(screen.getByLabelText("Keyword"));
    await user.type(screen.getByLabelText("Trace ID"), "trace-billing-1");
    expect(screen.getByText("Invoice retry queue is growing faster than the worker drain rate.")).toBeInTheDocument();
    expect(screen.queryByText("Order checkout completed successfully.")).not.toBeInTheDocument();
  });

  it("pauses, resumes, and clears visible rows", async () => {
    const user = userEvent.setup();
    renderLiveLogsPage();

    await screen.findByText("Payment gateway timeout exceeded the critical threshold.");
    await user.click(screen.getByRole("button", { name: "Pause" }));
    expect(screen.getByText("Paused")).toBeInTheDocument();
    expect(screen.getByText("Payment gateway timeout exceeded the critical threshold.")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Resume" }));
    expect(screen.getByText("Live")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Clear" }));
    expect(screen.getByText("No logs match the current filters.")).toBeInTheDocument();
  });

  it("opens the detail drawer from a selected row", async () => {
    const user = userEvent.setup();
    renderLiveLogsPage();

    await user.click(
      await screen.findByText(
        "Payment gateway timeout exceeded the critical threshold."
      )
    );

    expect(screen.getByLabelText("Log details")).toBeInTheDocument();
    expect(screen.getByText("evt-1001")).toBeInTheDocument();
    expect(screen.getByText("ing-9001")).toBeInTheDocument();
    expect(screen.getAllByText("trace-checkout-1")).not.toHaveLength(0);
    expect(screen.getByText("ap-southeast-1")).toBeInTheDocument();
  });

  it("renders a reset action for empty filter results", async () => {
    const user = userEvent.setup();
    renderLiveLogsPage();

    await screen.findByText("Payment gateway timeout exceeded the critical threshold.");
    await user.type(screen.getByLabelText("Trace ID"), "missing-trace");
    expect(screen.getByText("No logs match the current filters.")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "Reset filters" })).toHaveLength(2);
  });

  it("renders the no-authorized-applications state", async () => {
    vi.mocked(getInitialLiveLogSnapshot).mockResolvedValueOnce({
      ...baseSnapshot,
      applications: [],
      entries: []
    });

    renderLiveLogsPage();

    expect(
      await screen.findByText(
        "No authorized applications are available for live monitoring yet."
      )
    ).toBeInTheDocument();
  });

  it("keeps rows visible for disconnected and error states", async () => {
    vi.mocked(getInitialLiveLogSnapshot).mockResolvedValueOnce({
      ...baseSnapshot,
      connectionState: "disconnected"
    });
    renderLiveLogsPage();
    expect(
      await screen.findByText("Connection status: Disconnected. Existing rows remain visible.")
    ).toBeInTheDocument();
    expect(screen.getByText("Payment gateway timeout exceeded the critical threshold.")).toBeInTheDocument();

    vi.mocked(getInitialLiveLogSnapshot).mockResolvedValueOnce({
      ...baseSnapshot,
      connectionState: "error"
    });
    renderLiveLogsPage();
    expect(
      await screen.findByText("Connection status: Error. Existing rows remain visible.")
    ).toBeInTheDocument();
  });

  it("renders retryable error state when the adapter fails", async () => {
    vi.mocked(getInitialLiveLogSnapshot).mockRejectedValueOnce(new Error("boom"));
    renderLiveLogsPage();

    expect(
      await screen.findByText("Unable to load live logs right now. Please try again.")
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Retry" })).toBeInTheDocument();
  });

  it("caps visible rows and exposes overflow in buffered counts", async () => {
    vi.mocked(getInitialLiveLogSnapshot).mockResolvedValueOnce({
      ...baseSnapshot,
      entries: Array.from({ length: MAX_VISIBLE_LOGS + 5 }, (_, index) => ({
        id: `log-${index + 1}`,
        timestamp: `2026-06-09T10:15:${String(index).padStart(2, "0")}Z`,
        applicationId: "checkout-api",
        applicationName: "checkout-api",
        level: "INFO" as const,
        message: `Synthetic log ${index + 1}`,
        traceId: `trace-${index + 1}`
      })),
      buffered: 0,
      dropped: 0
    });

    renderLiveLogsPage();

    await screen.findByText("Synthetic log 1");
    expect(screen.getAllByRole("row")).toHaveLength(MAX_VISIBLE_LOGS + 1);
    expect(screen.getByText("Buffered: 5")).toBeInTheDocument();
  });

  it("renders adapter-reported buffered and dropped counts", async () => {
    renderLiveLogsPage();

    expect(await screen.findByText("Buffered: 2")).toBeInTheDocument();
    expect(screen.getByText("Dropped: 1")).toBeInTheDocument();
  });
});
