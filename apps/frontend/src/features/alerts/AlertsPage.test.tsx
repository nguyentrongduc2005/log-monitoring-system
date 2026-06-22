import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import { AlertCenterContext, type AlertCenterValue } from "./alert-center-context";
import { Component as AlertsPage } from "./AlertsPage";
import type { Alert } from "./alerts-types";

const alert: Alert = {
  id: "alert-1",
  ruleId: "rule-1",
  applicationId: "app-1",
  applicationName: "payment-service",
  applicationDisplayName: "Payment Service",
  severity: "ERROR",
  message: "Payment gateway timed out",
  fingerprint: "payment-timeout-fingerprint",
  logTimestamp: "2026-06-22T14:00:00Z",
  triggeredAt: "2026-06-22T14:00:01Z",
  status: "OPEN"
};

function renderPage(overrides: Partial<AlertCenterValue> = {}) {
  const value: AlertCenterValue = {
    alerts: [alert], applications: [{ id: "app-1", name: "Payment Service" }],
    connectionState: "live", error: null, loading: false, openCount: 1, saving: false,
    refresh: vi.fn(), acknowledge: vi.fn().mockResolvedValue(undefined), resolve: vi.fn().mockResolvedValue(undefined),
    ...overrides
  };
  render(<AlertCenterContext.Provider value={value}><PageHeaderProvider><AlertsPage /></PageHeaderProvider></AlertCenterContext.Provider>);
  return value;
}

describe("AlertsPage", () => {
  it("shows authorized application alerts and acknowledges an open alert", async () => {
    const user = userEvent.setup();
    const center = renderPage();
    expect(screen.getByText("Payment gateway timed out")).toBeInTheDocument();
    expect(screen.getAllByText("Payment Service")).toHaveLength(2);
    await user.click(screen.getByRole("button", { name: "Acknowledge" }));
    await waitFor(() => expect(center.acknowledge).toHaveBeenCalledWith("alert-1"));
  });

  it("filters alerts by status", async () => {
    const user = userEvent.setup();
    renderPage();
    await user.selectOptions(screen.getByLabelText("Filter alert status"), "RESOLVED");
    expect(screen.getByText("No alerts match the current filters.")).toBeInTheDocument();
  });
});
