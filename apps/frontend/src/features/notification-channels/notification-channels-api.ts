import { apiClient } from "@/api/client";
import { getAlertingError } from "@/features/alert-rules/alert-rules-adapter";
import type {
  ApiEnvelope,
  ChatRoom,
  ChatRoomStatus
} from "@/features/alert-rules/alert-rules-types";

export { getAlertingError as getNotificationChannelError };

export type CreateChatRoomRequest = {
  channel: "TELEGRAM";
  name: string;
  chatId: string;
  description?: string;
};

export type TelegramChat = {
  chatId: string;
  name: string;
  type: string;
  username?: string | null;
};

function requireData<T>(envelope: ApiEnvelope<T>, fallbackMessage: string): T {
  if (envelope.data === undefined || envelope.data === null) {
    throw new Error(envelope.message || fallbackMessage);
  }
  return envelope.data;
}

export async function getTelegramChatRooms(
  status?: ChatRoomStatus
): Promise<ChatRoom[]> {
  const response = await apiClient.get<ApiEnvelope<ChatRoom[]>>(
    "/alert-chat-rooms",
    { params: { channel: "TELEGRAM", ...(status ? { status } : {}) } }
  );
  return requireData(response.data, "Unable to load Telegram chat rooms.");
}

export async function createTelegramChatRoom(
  request: Omit<CreateChatRoomRequest, "channel">
): Promise<ChatRoom> {
  const response = await apiClient.post<ApiEnvelope<ChatRoom>>(
    "/alert-chat-rooms",
    { ...request, channel: "TELEGRAM" }
  );
  return requireData(response.data, "Unable to create Telegram chat room.");
}

export async function changeChatRoomStatus(
  id: string,
  status: ChatRoomStatus
): Promise<ChatRoom> {
  const response = await apiClient.put<ApiEnvelope<ChatRoom>>(
    `/alert-chat-rooms/${id}/status`,
    { status }
  );
  return requireData(response.data, "Unable to update chat room status.");
}

export async function discoverTelegramChats(): Promise<TelegramChat[]> {
  const response = await apiClient.get<ApiEnvelope<TelegramChat[]>>(
    "/alert-chat-rooms/telegram/discover"
  );
  return requireData(response.data, "Unable to discover Telegram chats.");
}
