import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/api/client";
import { createTelegramChatRoom } from "./notification-channels-api";

vi.mock("@/api/client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }
}));

describe("notification channels API", () => {
  beforeEach(() => vi.clearAllMocks());

  it("always registers a chat room as Telegram", async () => {
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: { id: "room-id" } } });

    await createTelegramChatRoom({
      name: "Ops critical",
      chatId: "-100123456",
      description: "On-call group"
    });

    expect(apiClient.post).toHaveBeenCalledWith("/alert-chat-rooms", {
      name: "Ops critical",
      chatId: "-100123456",
      description: "On-call group",
      channel: "TELEGRAM"
    });
  });
});
