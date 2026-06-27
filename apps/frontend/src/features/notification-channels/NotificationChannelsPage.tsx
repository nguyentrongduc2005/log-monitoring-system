import { useEffect, useMemo, useState, type FormEvent } from "react";
import {
  managementButtonClass,
  managementInputClass,
  managementPanelClass,
  managementPrimaryButtonClass,
  MetricCard,
  StatusBadge
} from "@/shared/components/management-ui";
import { PageHeader } from "@/shared/layouts/page-header-context";
import type { ChatRoom, ChatRoomStatus } from "@/features/alert-rules/alert-rules-types";
import {
  changeChatRoomStatus,
  createTelegramChatRoom,
  discoverTelegramChats,
  getNotificationChannelError,
  getTelegramChatRooms,
  type TelegramChat
} from "./notification-channels-api";

export function Component() {
  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [discoveredChats, setDiscoveredChats] = useState<TelegramChat[]>([]);
  const [name, setName] = useState("");
  const [chatId, setChatId] = useState("");
  const [description, setDescription] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<"ALL" | ChatRoomStatus>("ALL");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [discovering, setDiscovering] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [discoveryError, setDiscoveryError] = useState<string | null>(null);

  async function loadRooms() {
    setLoading(true);
    setError(null);
    try {
      setRooms(await getTelegramChatRooms());
    } catch (loadError) {
      setError(
        getNotificationChannelError(loadError, "Unable to load notification channels.")
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => void loadRooms());
  }, []);

  const filteredRooms = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    return rooms.filter(room =>
      (statusFilter === "ALL" || room.status === statusFilter) &&
      (!normalized ||
        `${room.name} ${room.chatId} ${room.description || ""}`
          .toLowerCase()
          .includes(normalized))
    );
  }, [rooms, search, statusFilter]);

  async function discoverChats() {
    setDiscovering(true);
    setDiscoveryError(null);
    setDiscoveredChats([]);
    try {
      setDiscoveredChats(await discoverTelegramChats());
    } catch (discoverError) {
      setDiscoveryError(
        getNotificationChannelError(discoverError, "Unable to discover Telegram groups.")
      );
    } finally {
      setDiscovering(false);
    }
  }

  function selectDiscoveredChat(value: string) {
    const chat = discoveredChats.find(item => item.chatId === value);
    if (!chat) return;
    setChatId(chat.chatId);
    setName(chat.name);
  }

  async function createRoom(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      const created = await createTelegramChatRoom({
        name: name.trim(),
        chatId: chatId.trim(),
        description: description.trim() || undefined
      });
      setRooms(current => [created, ...current]);
      setName("");
      setChatId("");
      setDescription("");
    } catch (saveError) {
      setError(
        getNotificationChannelError(saveError, "Unable to create Telegram chat room.")
      );
    } finally {
      setSaving(false);
    }
  }

  async function toggleRoom(room: ChatRoom) {
    setSaving(true);
    setError(null);
    try {
      const updated = await changeChatRoomStatus(
        room.id,
        room.status === "ACTIVE" ? "DISABLED" : "ACTIVE"
      );
      setRooms(current => current.map(item => (item.id === updated.id ? updated : item)));
    } catch (actionError) {
      setError(
        getNotificationChannelError(actionError, "Unable to update Telegram chat room.")
      );
    } finally {
      setSaving(false);
    }
  }

  const activeRooms = rooms.filter(room => room.status === "ACTIVE").length;

  return (
    <div className="space-y-5">
      <PageHeader title="Notification Channels" />
      <p className="text-sm text-muted">
        Register Telegram groups that can receive alert notifications.
      </p>

      <section className="grid gap-3 md:grid-cols-3">
        <MetricCard label="Telegram rooms" value={rooms.length} />
        <MetricCard label="Active rooms" tone="success" value={activeRooms} />
        <MetricCard label="Disabled rooms" tone="muted" value={rooms.length - activeRooms} />
      </section>

      <form className={managementPanelClass} onSubmit={event => void createRoom(event)}>
        <div className="flex flex-col gap-3 border-b border-border bg-surface-raised/35 p-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-semibold text-text">Register Telegram room</h2>
            <p className="mt-1 text-sm text-muted">
              Choose a group discovered by the bot or enter its chat ID manually.
            </p>
          </div>
          <button
            className={managementButtonClass}
            disabled={discovering}
            onClick={() => void discoverChats()}
            type="button"
          >
            {discovering ? "Discovering..." : "Discover groups"}
          </button>
        </div>

        <div className="grid gap-4 p-4 lg:grid-cols-2 xl:grid-cols-4">
          <label className="block">
            <span className="text-sm font-medium text-text">Discovered group</span>
            <select
              aria-label="Discovered Telegram group"
              className={`mt-2 ${managementInputClass}`}
              onChange={event => selectDiscoveredChat(event.target.value)}
              value={discoveredChats.some(chat => chat.chatId === chatId) ? chatId : ""}
            >
              <option value="">Select a discovered group</option>
              {discoveredChats.map(chat => (
                <option key={chat.chatId} value={chat.chatId}>
                  {chat.name} ({chat.type})
                </option>
              ))}
            </select>
          </label>
          <label className="block">
            <span className="text-sm font-medium text-text">Room name</span>
            <input
              className={`mt-2 ${managementInputClass}`}
              maxLength={120}
              onChange={event => setName(event.target.value)}
              placeholder="Operations critical"
              required
              value={name}
            />
          </label>
          <label className="block">
            <span className="text-sm font-medium text-text">Telegram chat ID</span>
            <input
              className={`mt-2 ${managementInputClass}`}
              maxLength={128}
              onChange={event => setChatId(event.target.value)}
              placeholder="-100123456789"
              required
              value={chatId}
            />
          </label>
          <label className="block">
            <span className="text-sm font-medium text-text">Description</span>
            <input
              className={`mt-2 ${managementInputClass}`}
              maxLength={2000}
              onChange={event => setDescription(event.target.value)}
              placeholder="Optional purpose or owner"
              value={description}
            />
          </label>
        </div>
        {discoveredChats.length > 0 ? (
          <div className="mx-4 mb-4 rounded-lg border border-primary/25 bg-primary/5 p-3">
            <p className="text-sm font-medium text-text">
              Found {discoveredChats.length} Telegram {discoveredChats.length === 1 ? "group" : "groups"}
            </p>
            <div className="mt-3 grid gap-2 md:grid-cols-2">
              {discoveredChats.map(chat => {
                const selected = chat.chatId === chatId;
                return (
                  <button
                    aria-pressed={selected}
                    className={`flex items-center justify-between gap-3 rounded-md border px-3 py-2 text-left transition ${
                      selected
                        ? "border-primary bg-primary/10"
                        : "border-border bg-background hover:border-primary/60"
                    }`}
                    key={chat.chatId}
                    onClick={() => selectDiscoveredChat(chat.chatId)}
                    type="button"
                  >
                    <span className="min-w-0">
                      <span className="block truncate text-sm font-medium text-text">
                        {chat.name}
                      </span>
                      <span className="mt-0.5 block text-xs text-muted">
                        {chat.type} · {chat.chatId}
                      </span>
                    </span>
                    <span className="shrink-0 text-xs font-semibold text-primary">
                      {selected ? "Selected" : "Use group"}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        ) : null}
        {!discovering && discoveredChats.length === 0 && !discoveryError ? (
          <p className="mx-4 mb-4 text-xs text-muted">
            Click Discover groups to load Telegram groups visible to the configured bot.
          </p>
        ) : null}
        {discoveryError ? (
          <p className="mx-4 mb-4 rounded-md border border-warning/30 bg-warning/10 px-3 py-2 text-sm text-warning">
            {discoveryError}
          </p>
        ) : null}
        <div className="flex justify-end border-t border-border p-4">
          <button
            className={managementPrimaryButtonClass}
            disabled={saving || !name.trim() || !chatId.trim()}
            type="submit"
          >
            {saving ? "Saving..." : "Register room"}
          </button>
        </div>
      </form>

      <section className={managementPanelClass}>
        <div className="flex flex-col gap-3 border-b border-border bg-surface-raised/35 p-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="font-semibold text-text">Telegram rooms</h2>
            <p className="mt-1 text-sm text-muted">Only active rooms can be assigned to alert rules.</p>
          </div>
          <div className="flex flex-wrap gap-2">
            <input
              aria-label="Search notification channels"
              className={managementInputClass}
              onChange={event => setSearch(event.target.value)}
              placeholder="Search rooms..."
              value={search}
            />
            <select
              aria-label="Filter chat room status"
              className={managementInputClass}
              onChange={event => setStatusFilter(event.target.value as "ALL" | ChatRoomStatus)}
              value={statusFilter}
            >
              <option value="ALL">All rooms</option>
              <option value="ACTIVE">Active</option>
              <option value="DISABLED">Disabled</option>
            </select>
            <button className={managementButtonClass} onClick={() => void loadRooms()} type="button">
              Refresh
            </button>
          </div>
        </div>

        {loading ? <p className="p-4 text-sm text-muted">Loading Telegram rooms...</p> : null}
        {error ? (
          <p className="m-4 rounded-md border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">{error}</p>
        ) : null}
        {!loading ? (
          <div className="divide-y divide-border">
            {filteredRooms.map(room => (
              <article className="grid gap-3 p-4 md:grid-cols-[1fr_1fr_10rem_auto] md:items-center" key={room.id}>
                <div>
                  <p className="font-semibold text-text">{room.name}</p>
                  <p className="mt-1 text-xs text-muted">{room.description || "No description"}</p>
                </div>
                <code className="text-sm text-primary">{room.chatId}</code>
                <StatusBadge tone={room.status === "ACTIVE" ? "success" : "muted"}>{room.status}</StatusBadge>
                <button
                  className={managementButtonClass}
                  disabled={saving}
                  onClick={() => void toggleRoom(room)}
                  type="button"
                >
                  {room.status === "ACTIVE" ? "Disable" : "Enable"}
                </button>
              </article>
            ))}
            {filteredRooms.length === 0 ? (
              <p className="px-5 py-10 text-center text-sm text-muted">No Telegram rooms found.</p>
            ) : null}
          </div>
        ) : null}
      </section>
    </div>
  );
}
