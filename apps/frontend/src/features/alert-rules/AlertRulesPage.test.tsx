import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import { Component as AlertRulesPage } from "./AlertRulesPage";
import {
  getAlertRules,
  saveAlertRule,
  toggleAlertRule
} from "./alert-rules-adapter";
import type { AlertRule } from "./alert-rules-types";

vi.mock("./alert-rules-adapter", () => ({
  getAlertRules: vi.fn(),
  saveAlertRule: vi.fn(),
  toggleAlertRule: vi.fn()
}));

const criticalRule: AlertRule = {
  id: "rule-auth-401-flood",
  name: "Auth-401-Flood",
  applicationName: "Payment Gateway",
  serviceName: "auth-service",
  severity: "CRITICAL",
  metric: "LOG_COUNT",
  operator: ">",
  threshold: 500,
  windowSeconds: 30,
  channelType: "Telegram",
  channelTarget: "#ops-critical",
  status: "RUNNING",
  triggered24h: 14,
  breached24h: 44
};

function renderPage() {
  return render(
    <PageHeaderProvider>
      <AlertRulesPage />
    </PageHeaderProvider>
  );
}

describe("AlertRulesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getAlertRules).mockResolvedValue([criticalRule]);
    vi.mocked(saveAlertRule).mockResolvedValue({
      ...criticalRule,
      id: "rule-new",
      name: "Gateway errors",
      serviceName: "api-gateway",
      severity: "ERROR",
      threshold: 10,
      triggered24h: 0,
      breached24h: 0
    });
    vi.mocked(toggleAlertRule).mockResolvedValue({
      ...criticalRule,
      status: "MUTED"
    });
  });

  it("loads alert rules and mutes a rule", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Auth-401-Flood")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Mute" }));

    await waitFor(() =>
      expect(toggleAlertRule).toHaveBeenCalledWith("rule-auth-401-flood")
    );
  });

  it("creates an alert rule from the builder", async () => {
    const user = userEvent.setup();
    renderPage();

    await screen.findByText("Auth-401-Flood");
    const form = screen.getByRole("button", { name: "Create Rule" }).closest("form");
    expect(form).not.toBeNull();
    await user.clear(within(form!).getByLabelText("Rule name"));
    await user.type(within(form!).getByLabelText("Rule name"), "Gateway errors");
    await user.clear(within(form!).getByLabelText("Service boundary"));
    await user.type(within(form!).getByLabelText("Service boundary"), "api-gateway");
    await user.selectOptions(within(form!).getByLabelText("Metric"), "LOG_COUNT");
    await user.clear(within(form!).getByLabelText("Threshold"));
    await user.type(within(form!).getByLabelText("Threshold"), "10");
    await user.click(within(form!).getByRole("button", { name: "Create Rule" }));

    await waitFor(() =>
      expect(saveAlertRule).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "Gateway errors",
          serviceName: "api-gateway",
          severity: "CRITICAL",
          metric: "LOG_COUNT",
          threshold: 10,
          channelTarget: "#ops-critical"
        }),
        undefined
      )
    );
  });
});
