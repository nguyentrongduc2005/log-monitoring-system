import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/api/client";
import { saveAlertRule } from "./alert-rules-adapter";
import type { AlertRuleDraft } from "./alert-rules-types";

vi.mock("@/api/client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}));

const draft: AlertRuleDraft = {
  applicationId: "00000000-0000-0000-0000-000000000101",
  name: "Gateway errors",
  description: "",
  minSeverity: "ERROR",
  keywordPattern: "timeout",
  thresholdCount: 5,
  thresholdWindowSeconds: 60,
  cooldownSeconds: 300,
  websocketEnabled: true,
  telegramChatRoomIds: [
    "00000000-0000-0000-0000-000000000201",
    "00000000-0000-0000-0000-000000000202"
  ]
};

describe("alert rules API adapter", () => {
  beforeEach(() => vi.clearAllMocks());

  it("maps WebSocket and multiple Telegram rooms to delivery targets", async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: { id: "rule-id" } } });

    await saveAlertRule(draft);

    expect(apiClient.post).toHaveBeenCalledWith("/alert-rules", {
      applicationId: draft.applicationId,
      name: "Gateway errors",
      description: undefined,
      minSeverity: "ERROR",
      keywordPattern: "timeout",
      thresholdCount: 5,
      thresholdWindowSeconds: 60,
      cooldownSeconds: 300,
      deliveryTargets: [
        { channel: "WEBSOCKET", chatRoomId: null },
        { channel: "TELEGRAM", chatRoomId: draft.telegramChatRoomIds[0] },
        { channel: "TELEGRAM", chatRoomId: draft.telegramChatRoomIds[1] }
      ]
    });
  });

  it("omits applicationId from an update request", async () => {
    vi.mocked(apiClient.put).mockResolvedValue({ data: { data: { id: "rule-id" } } });

    await saveAlertRule(draft, "rule-id");

    expect(apiClient.put).toHaveBeenCalledWith(
      "/alert-rules/rule-id",
      expect.not.objectContaining({ applicationId: expect.anything() })
    );
  });
});
