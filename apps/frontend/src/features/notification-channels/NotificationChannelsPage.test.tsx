import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import { Component as NotificationChannelsPage } from "./NotificationChannelsPage";
import {
  changeChatRoomStatus,
  createTelegramChatRoom,
  discoverTelegramChats,
  getTelegramChatRooms
} from "./notification-channels-api";
import type { ChatRoom } from "@/features/alert-rules/alert-rules-types";

vi.mock("./notification-channels-api", async importOriginal => {
  const actual = await importOriginal<typeof import("./notification-channels-api")>();
  return {
    ...actual,
    getTelegramChatRooms: vi.fn(),
    createTelegramChatRoom: vi.fn(),
    changeChatRoomStatus: vi.fn(),
    discoverTelegramChats: vi.fn()
  };
});

const room: ChatRoom = {
  id: "00000000-0000-0000-0000-000000000201",
  channel: "TELEGRAM",
  name: "Ops critical",
  chatId: "-100123456",
  status: "ACTIVE",
  createdAt: "2026-06-01T10:00:00Z",
  updatedAt: "2026-06-01T10:00:00Z"
};

function renderPage() {
  return render(
    <PageHeaderProvider>
      <NotificationChannelsPage />
    </PageHeaderProvider>
  );
}

describe("NotificationChannelsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getTelegramChatRooms).mockResolvedValue([room]);
    vi.mocked(discoverTelegramChats).mockResolvedValue([
      { chatId: "-100999", name: "Platform alerts", type: "SUPERGROUP" }
    ]);
    vi.mocked(createTelegramChatRoom).mockResolvedValue({
      ...room,
      id: "00000000-0000-0000-0000-000000000202",
      name: "Platform alerts",
      chatId: "-100999"
    });
    vi.mocked(changeChatRoomStatus).mockResolvedValue({
      ...room,
      status: "DISABLED"
    });
  });

  it("discovers and registers a Telegram group", async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Ops critical");

    await user.click(screen.getByRole("button", { name: "Discover groups" }));
    await waitFor(() => expect(discoverTelegramChats).toHaveBeenCalled());
    await user.selectOptions(
      screen.getByLabelText("Discovered Telegram group"),
      "-100999"
    );
    const form = screen.getByRole("button", { name: "Register room" }).closest("form")!;
    await user.click(within(form).getByRole("button", { name: "Register room" }));

    await waitFor(() =>
      expect(createTelegramChatRoom).toHaveBeenCalledWith({
        name: "Platform alerts",
        chatId: "-100999",
        description: undefined
      })
    );
  });

  it("disables an active Telegram room", async () => {
    const user = userEvent.setup();
    renderPage();
    await screen.findByText("Ops critical");
    await user.click(screen.getByRole("button", { name: "Disable" }));

    await waitFor(() =>
      expect(changeChatRoomStatus).toHaveBeenCalledWith(room.id, "DISABLED")
    );
  });
});
