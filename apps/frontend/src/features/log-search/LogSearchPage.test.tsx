import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import { Component as LogSearchPage } from "./LogSearchPage";
import type { LogSearchFilters } from "./log-search-types";

vi.mock("@/features/log-search/log-search-adapter", () => {
  return {
    searchLogs: vi.fn(async (filters: LogSearchFilters) => {
      if (filters.query === "invoice") {
        return {
          applications: [{ id: "billing-worker", name: "Billing Worker" }],
          summary: {
            totalMatches: 1,
            errorMatches: 1,
            criticalMatches: 0,
            uniqueTraces: 1,
            slowestDurationMs: 0,
          },
          buckets: [],
          results: [
            {
              id: "log-2",
              timestamp: "2026-07-03 08:04:35.188",
              applicationId: "billing-worker",
              applicationName: "Billing Worker",
              level: "ERROR",
              message: "Invoice retry storm detected for batch INV-BATCH-118",
              traceId: "trc-inv-118",
              spanId: "",
              eventId: "log-2",
              source: "invoice.scheduler",
              host: "",
              attributes: {},
              stack: ["com.vdt.BillingWorker.execute(BillingWorker.java:45)"],
            },
          ],
          relatedTrace: [],
        };
      }
      return {
        applications: [{ id: "checkout-api", name: "Checkout API" }],
        summary: {
          totalMatches: 1,
          errorMatches: 1,
          criticalMatches: 0,
          uniqueTraces: 1,
          slowestDurationMs: 0,
        },
        buckets: [],
        results: [
          {
            id: "log-1",
            timestamp: "2026-07-03 08:04:35.174",
            applicationId: "checkout-api",
            applicationName: "Checkout API",
            level: "ERROR",
            message: "Payment gateway timeout after 3000ms while authorizing order ORD-8842",
            traceId: "trc-pay-8842",
            spanId: "",
            eventId: "log-1",
            source: "",
            host: "",
            attributes: {},
            stack: ["at com.vdt.CheckoutService.authorize(CheckoutService.java:23)"],
          },
        ],
        relatedTrace: [
          {
            id: "log-1",
            timestamp: "2026-07-03 08:04:35.174",
            applicationId: "checkout-api",
            applicationName: "Checkout API",
            level: "ERROR",
            message: "Payment gateway timeout after 3000ms while authorizing order ORD-8842",
            traceId: "trc-pay-8842",
            spanId: "",
            eventId: "log-1",
            source: "",
            host: "",
            attributes: {},
            stack: ["at com.vdt.CheckoutService.authorize(CheckoutService.java:23)"],
          },
        ],
      };
    }),
  };
});

function renderPage() {
  render(
    <PageHeaderProvider>
      <LogSearchPage />
    </PageHeaderProvider>,
  );
}

describe("LogSearchPage", () => {
  it("renders searchable log results with filters and trace context", async () => {
    renderPage();

    expect(await screen.findAllByText("Log Search")).not.toHaveLength(0);
    expect(screen.getByLabelText("Search query")).toHaveValue("timeout");
    expect(
      screen.getByText(
        "Search in message, traceId, eventId, source, host, attributes",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "traceId:trc-pay-8842" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Log volume by severity"),
    ).not.toBeInTheDocument();
    expect(
      screen.getAllByText(
        "Payment gateway timeout after 3000ms while authorizing order ORD-8842",
      ),
    ).not.toHaveLength(0);
    expect(screen.getByText("Error trace")).toBeInTheDocument();
    expect(screen.getByText("trc-pay-8842")).toBeInTheDocument();
    expect(screen.getByText("Stack trace")).toBeInTheDocument();
  });

  it("searches another failure and updates the detail panel", async () => {
    const user = userEvent.setup();
    renderPage();

    const query = await screen.findByLabelText("Search query");
    await user.clear(query);
    await user.type(query, "invoice");
    await user.click(screen.getByRole("button", { name: "Search" }));

    await waitFor(() =>
      expect(
        screen.getAllByText(
          "Invoice retry storm detected for batch INV-BATCH-118",
        ),
      ).not.toHaveLength(0),
    );
    expect(screen.getAllByText("Billing Worker")).not.toHaveLength(0);
    expect(screen.getByText("trc-inv-118")).toBeInTheDocument();
    expect(screen.getAllByText("invoice.scheduler")).not.toHaveLength(0);
  });
});
