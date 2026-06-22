import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import { Component as AlertRulesPage } from "./AlertRulesPage";
import {
  deleteAlertRule,
  getAlertRules,
  getTelegramChatRooms,
  saveAlertRule,
  toggleAlertRule
} from "./alert-rules-adapter";
import { getApplications } from "@/features/applications/application-api";
import type { AlertRule, ChatRoom } from "./alert-rules-types";

vi.mock("./alert-rules-adapter", async importOriginal => {
  const actual = await importOriginal<typeof import("./alert-rules-adapter")>();
  return {
    ...actual,
    getAlertRules: vi.fn(),
    getTelegramChatRooms: vi.fn(),
    saveAlertRule: vi.fn(),
    toggleAlertRule: vi.fn(),
    deleteAlertRule: vi.fn()
  };
});

vi.mock("@/features/applications/application-api", async importOriginal => {
  const actual = await importOriginal<
    typeof import("@/features/applications/application-api")
  >();
  return { ...actual, getApplications: vi.fn() };
});

const application = {
  id: "00000000-0000-0000-0000-000000000101",
  name: "billing-service",
  displayName: "Billing Service",
  status: "ACTIVE"
};

const room: ChatRoom = {
  id: "00000000-0000-0000-0000-000000000201",
  channel: "TELEGRAM",
  name: "Ops critical",
  chatId: "-100123456",
  status: "ACTIVE",
  createdAt: "2026-06-01T10:00:00Z",
  updatedAt: "2026-06-01T10:00:00Z"
};

const criticalRule: AlertRule = {
  id: "00000000-0000-0000-0000-000000000301",
  applicationId: application.id,
  name: "Auth 401 flood",
  description: "Detect authentication failures",
  minSeverity: "ERROR",
  keywordPattern: "unauthorized",
  thresholdCount: 10,
  thresholdWindowSeconds: 60,
  cooldownSeconds: 300,
  status: "ACTIVE",
  channels: ["WEBSOCKET", "TELEGRAM"],
  deliveryTargets: [
    { channel: "WEBSOCKET", chatRoomId: null },
    { channel: "TELEGRAM", chatRoomId: room.id }
  ],
  createdBy: "00000000-0000-0000-0000-000000000401",
  createdAt: "2026-06-01T10:00:00Z",
  updatedAt: "2026-06-01T10:00:00Z"
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
    vi.mocked(getApplications).mockResolvedValue([application]);
    vi.mocked(getTelegramChatRooms).mockResolvedValue([room]);
    vi.mocked(saveAlertRule).mockResolvedValue({
      ...criticalRule,
      id: "00000000-0000-0000-0000-000000000302",
      name: "Gateway errors"
    });
    vi.mocked(toggleAlertRule).mockResolvedValue({
      ...criticalRule,
      status: "DISABLED"
    });
    vi.mocked(deleteAlertRule).mockResolvedValue();
  });

  it("loads alert rules and disables a rule", async () => {
    const user = userEvent.setup();
    renderPage();

    expect(await screen.findByText("Auth 401 flood")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Disable" }));

    await waitFor(() => expect(toggleAlertRule).toHaveBeenCalledWith(criticalRule));
  });

  it("creates a rule with WebSocket and a Telegram room", async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Auth 401 flood");

    const form = screen.getByRole("button", { name: "Create rule" }).closest("form")!;
    await user.type(within(form).getByLabelText("Rule name"), "Gateway errors");
    await user.click(
      within(form).getByRole("checkbox", { name: /Ops critical/ })
    );
    await user.click(within(form).getByRole("button", { name: "Create rule" }));

    await waitFor(() =>
      expect(saveAlertRule).toHaveBeenCalledWith(
        {
          applicationId: application.id,
          name: "Gateway errors",
          description: "",
          minSeverity: "ERROR",
          keywordPattern: "",
          thresholdCount: 1,
          thresholdWindowSeconds: 60,
          cooldownSeconds: 300,
          websocketEnabled: true,
          telegramChatRoomIds: [room.id]
        },
        undefined
      )
    );
  });

  it("confirms before deleting a rule", async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Auth 401 flood");

    await user.click(screen.getByRole("button", { name: "Delete" }));
    await user.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Delete rule" }));

    await waitFor(() => expect(deleteAlertRule).toHaveBeenCalledWith(criticalRule.id));
  });
});
