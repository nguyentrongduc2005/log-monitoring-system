import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import { AlertCenterContext, type AlertCenterValue } from "./alert-center-context";
import { Component as AlertsPage } from "./AlertsPage";
import type { Alert } from "./alerts-types";

const mocks = vi.hoisted(() => ({
  startIncidentFromAlert: vi.fn()
}));

vi.mock("@/features/incidents/incident-api", () => ({
  startIncidentFromAlert: (alertId: string) => mocks.startIncidentFromAlert(alertId)
}));

const alert: Alert = {
  id: "alert-1",
  ruleId: "11111111-1111-1111-1111-111111111111",
  ruleName: "Payment gateway failures",
  applicationId: "app-1",
  applicationName: "payment-service",
  applicationDisplayName: "Payment Service",
  severity: "ERROR",
  logSamples: [{ level: "ERROR", message: "Payment gateway timed out" }],
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
  render(<MemoryRouter><AlertCenterContext.Provider value={value}><PageHeaderProvider><AlertsPage /></PageHeaderProvider></AlertCenterContext.Provider></MemoryRouter>);
  return value;
}

describe("AlertsPage", () => {
  beforeEach(() => {
    mocks.startIncidentFromAlert.mockReset();
  });

  it("shows authorized application alerts and acknowledges an open alert", async () => {
    const user = userEvent.setup();
    const center = renderPage();
    mocks.startIncidentFromAlert.mockResolvedValue({ id: "incident-1" });
    expect(screen.getByText("Payment gateway failures")).toBeInTheDocument();
    expect(screen.getByText("Payment Service")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Acknowledge" }));
    await waitFor(() => expect(center.acknowledge).toHaveBeenCalledWith("alert-1"));
  });

  it("filters alerts by status", async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(screen.getByLabelText("Filter alert status"));
    await user.click(screen.getByRole("option", { name: "Resolved" }));
    expect(screen.getByText("No alerts match the current filters.")).toBeInTheDocument();
  });

  it("starts an incident from an alert", async () => {
    const user = userEvent.setup();
    mocks.startIncidentFromAlert.mockResolvedValue({ id: "incident-1" });
    renderPage();
    await user.click(screen.getByRole("button", { name: "Start Incident" }));
    await waitFor(() => expect(mocks.startIncidentFromAlert).toHaveBeenCalledWith("alert-1"));
  });

  it("expands alert details inline without exposing rule UUIDs", async () => {
    const user = userEvent.setup();
    renderPage();
    await user.click(screen.getByRole("button", { name: /Payment gateway failures/i }));
    expect(screen.getByText("Alert details")).toBeInTheDocument();
    expect(screen.getByText("Log Samples")).toBeInTheDocument();
    expect(screen.queryByText("11111111-1111-1111-1111-111111111111")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: /Payment gateway failures/i }));
    expect(screen.queryByText("Alert details")).not.toBeInTheDocument();
  });
});
