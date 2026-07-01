import { useState, type FormEvent } from "react";
import type { Application } from "@/features/applications/application-types";
import { cn } from "@/shared/lib/utils";
import {
  managementButtonClass,
  managementInputClass,
  managementPanelClass,
  managementPrimaryButtonClass
} from "@/shared/components/management-ui";
import type {
  AlertRule,
  AlertRuleDraft,
  AlertSeverity,
  ChatRoom
} from "../alert-rules-types";

const severityOptions: AlertSeverity[] = ["INFO", "WARN", "ERROR", "CRITICAL"];

type AlertRuleBuilderProps = {
  applications: Application[];
  chatRooms: ChatRoom[];
  draft: AlertRuleDraft;
  editingRule: AlertRule | null;
  saving: boolean;
  onDraftChange: (draft: AlertRuleDraft) => void;
  onReset: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export default function AlertRuleBuilder({
  applications,
  chatRooms,
  draft,
  editingRule,
  saving,
  onDraftChange,
  onReset,
  onSubmit
}: AlertRuleBuilderProps) {
  const [roomsDropdownOpen, setRoomsDropdownOpen] = useState(false);

  function updateDraft(update: Partial<AlertRuleDraft>) {
    onDraftChange({ ...draft, ...update });
  }

  function toggleTelegramRoom(roomId: string) {
    updateDraft({
      telegramChatRoomIds: draft.telegramChatRoomIds.includes(roomId)
        ? draft.telegramChatRoomIds.filter(id => id !== roomId)
        : [...draft.telegramChatRoomIds, roomId]
    });
  }

  const hasDeliveryTarget =
    draft.websocketEnabled || draft.telegramChatRoomIds.length > 0;
  const activeWindowInvalid =
    !draft.activeAllDay &&
    (!draft.activeStartTime ||
      !draft.activeEndTime ||
      draft.activeStartTime === draft.activeEndTime);

  return (
    <form className={managementPanelClass} onSubmit={onSubmit}>
      <div className="flex flex-col gap-3 border-b border-border bg-surface-raised/35 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-base font-semibold text-text">Rule builder</h2>
          <p className="mt-1 text-sm text-muted">
            Match log events and route alerts to one or more destinations.
          </p>
        </div>
        <span className="rounded-md border border-border bg-background px-2 py-1 text-xs font-semibold uppercase text-muted">
          {editingRule ? "Editing rule" : "New rule"}
        </span>
      </div>

      <div className="grid gap-6 p-5 lg:grid-cols-2">
        {/* Left Section: Metadata */}
        <div className="space-y-4">
          <h3 className="text-xs font-bold uppercase tracking-wider text-muted/70">Rule Identity</h3>
          <div className="space-y-4 rounded-lg border border-border bg-background/20 p-4">
            <label className="block">
              <span className="text-xs font-semibold text-muted">Application</span>
              <select
                className={`mt-1.5 ${managementInputClass}`}
                disabled={Boolean(editingRule)}
                onChange={event => updateDraft({ applicationId: event.target.value })}
                required
                value={draft.applicationId}
              >
                <option value="">Select an application</option>
                {applications.map(application => (
                  <option key={application.id} value={application.id}>
                    {application.displayName || application.name}
                  </option>
                ))}
              </select>
              {editingRule ? (
                <span className="mt-1 block text-[11px] text-muted/70 italic">
                  Application cannot be changed after creation.
                </span>
              ) : null}
            </label>

            <label className="block">
              <span className="text-xs font-semibold text-muted">Rule name</span>
              <input
                className={`mt-1.5 ${managementInputClass}`}
                maxLength={120}
                onChange={event => updateDraft({ name: event.target.value })}
                placeholder="Critical payment errors"
                required
                value={draft.name}
              />
            </label>

            <label className="block">
              <span className="text-xs font-semibold text-muted">Description</span>
              <textarea
                className={`mt-1.5 min-h-[5.5rem] ${managementInputClass}`}
                maxLength={2000}
                onChange={event => updateDraft({ description: event.target.value })}
                placeholder="Optional context for operators"
                value={draft.description}
              />
            </label>
          </div>
        </div>

        {/* Right Section: Conditions */}
        <div className="space-y-4">
          <h3 className="text-xs font-bold uppercase tracking-wider text-muted/70">Trigger Conditions</h3>
          <div className="space-y-4 rounded-lg border border-border bg-background/20 p-4">
            <div className="grid gap-4 sm:grid-cols-3">
              <label className="block">
                <span className="text-xs font-semibold text-muted">Minimum severity</span>
                <select
                  className={`mt-1.5 ${managementInputClass}`}
                  onChange={event =>
                    updateDraft({ minSeverity: event.target.value as AlertSeverity })
                  }
                  value={draft.minSeverity}
                >
                  {severityOptions.map(severity => (
                    <option key={severity} value={severity}>{severity}</option>
                  ))}
                </select>
              </label>
              
              <label className="block">
                <span className="text-xs font-semibold text-muted">Alert severity</span>
                <select
                  className={`mt-1.5 ${managementInputClass}`}
                  onChange={event =>
                    updateDraft({ severity: event.target.value as AlertSeverity })
                  }
                  value={draft.severity}
                >
                  {severityOptions.map(severity => (
                    <option key={severity} value={severity}>{severity}</option>
                  ))}
                </select>
              </label>

              <label className="block">
                <span className="text-xs font-semibold text-muted">Keyword pattern</span>
                <input
                  className={`mt-1.5 ${managementInputClass}`}
                  maxLength={255}
                  onChange={event => updateDraft({ keywordPattern: event.target.value })}
                  placeholder="Optional regex/text pattern"
                  value={draft.keywordPattern}
                />
              </label>
            </div>
            
            <div className="grid gap-3 grid-cols-3">
              <label className="block">
                <span className="text-xs font-semibold text-muted">Threshold count</span>
                <input
                  className={`mt-1.5 ${managementInputClass}`}
                  min={1}
                  onChange={event => updateDraft({ thresholdCount: Number(event.target.value) })}
                  required
                  type="number"
                  value={draft.thresholdCount}
                />
              </label>
              <label className="block">
                <span className="text-xs font-semibold text-muted">Window (s)</span>
                <input
                  className={`mt-1.5 ${managementInputClass}`}
                  min={1}
                  onChange={event =>
                    updateDraft({ thresholdWindowSeconds: Number(event.target.value) })
                  }
                  required
                  type="number"
                  value={draft.thresholdWindowSeconds}
                />
              </label>
              <label className="block">
                <span className="text-xs font-semibold text-muted">Cooldown (s)</span>
                <input
                  className={`mt-1.5 ${managementInputClass}`}
                  min={1}
                  onChange={event => updateDraft({ cooldownSeconds: Number(event.target.value) })}
                  required
                  type="number"
                  value={draft.cooldownSeconds}
                />
              </label>
            </div>

            <div className="rounded-md border border-border bg-background px-3 py-3">
              <label className="flex items-center gap-2 text-sm font-medium text-text">
                <input
                  checked={draft.activeAllDay}
                  className="size-4 rounded accent-primary"
                  onChange={event =>
                    updateDraft({
                      activeAllDay: event.target.checked,
                      ...(event.target.checked
                        ? { activeStartTime: "", activeEndTime: "" }
                        : {})
                    })
                  }
                  type="checkbox"
                />
                Active all day
              </label>
              {!draft.activeAllDay ? (
                <div className="mt-3 grid gap-3 sm:grid-cols-2">
                  <label className="block">
                    <span className="text-xs font-semibold text-muted">Start time</span>
                    <input
                      className={`mt-1.5 ${managementInputClass}`}
                      onChange={event => updateDraft({ activeStartTime: event.target.value })}
                      required
                      type="time"
                      value={draft.activeStartTime}
                    />
                  </label>
                  <label className="block">
                    <span className="text-xs font-semibold text-muted">End time</span>
                    <input
                      className={`mt-1.5 ${managementInputClass}`}
                      onChange={event => updateDraft({ activeEndTime: event.target.value })}
                      required
                      type="time"
                      value={draft.activeEndTime}
                    />
                  </label>
                  {draft.activeStartTime && draft.activeStartTime === draft.activeEndTime ? (
                    <p className="text-xs font-medium text-error sm:col-span-2">
                      Start time and end time must be different.
                    </p>
                  ) : null}
                </div>
              ) : null}
            </div>
          </div>
        </div>
      </div>

      <div className="border-t border-border p-5">
        <h3 className="text-xs font-bold uppercase tracking-wider text-muted/70 mb-4">Notification destinations</h3>
        <div className="grid gap-4 lg:grid-cols-2">
          {/* WebSocket Channel */}
          <label className={cn(
            "flex cursor-pointer items-start gap-3 rounded-lg border p-4 transition-all duration-200 hover:border-primary/50",
            draft.websocketEnabled ? "border-primary/45 bg-primary/5" : "border-border bg-background/20"
          )}>
            <input
              checked={draft.websocketEnabled}
              className="mt-1 size-4 accent-primary rounded"
              onChange={event => updateDraft({ websocketEnabled: event.target.checked })}
              type="checkbox"
            />
            <span>
              <span className="block text-sm font-semibold text-text">WebSocket Live Delivery</span>
              <span className="mt-1 block text-xs text-muted leading-relaxed">
                Publish alerts instantly to connected dashboard clients via WebSockets. No external chat integration required.
              </span>
            </span>
          </label>

          {/* Telegram Channel */}
          <div className={cn(
            "rounded-lg border p-4 transition-all duration-200 flex flex-col justify-between relative",
            draft.telegramChatRoomIds.length > 0 ? "border-primary/45 bg-primary/5" : "border-border bg-background/20"
          )}>
            <div className="flex items-start justify-between gap-2">
              <div>
                <p className="text-sm font-semibold text-text">Telegram Chat Routing</p>
                <p className="mt-0.5 text-xs text-muted mb-3">
                  Route notifications to one or more active chat rooms.
                </p>
              </div>
              {draft.telegramChatRoomIds.length > 0 ? (
                <span className="rounded bg-primary/20 px-1.5 py-0.5 text-[10px] font-bold text-primary-hover shrink-0">
                  {draft.telegramChatRoomIds.length} active
                </span>
              ) : null}
            </div>

            <div className="relative mt-auto">
              <button
                type="button"
                onClick={() => setRoomsDropdownOpen(!roomsDropdownOpen)}
                className="flex w-full items-center justify-between rounded-md border border-border bg-background px-3 py-2 text-xs text-text transition hover:border-primary/50 focus:outline-none cursor-pointer"
              >
                <span className="truncate pr-2">
                  {draft.telegramChatRoomIds.length === 0
                    ? "Select chat rooms"
                    : `Selected ${draft.telegramChatRoomIds.length} room${draft.telegramChatRoomIds.length === 1 ? "" : "s"}`}
                </span>
                <span className="text-muted text-[10px]">▼</span>
              </button>

              {roomsDropdownOpen ? (
                <>
                  <div 
                    className="fixed inset-0 z-10 cursor-default" 
                    onClick={() => setRoomsDropdownOpen(false)} 
                  />
                  <div className="absolute left-0 right-0 mt-1 z-20 max-h-36 overflow-y-auto rounded-md border border-border bg-[#141516] p-1.5 shadow-lg divide-y divide-border/40 shell-scrollbar">
                    {chatRooms.map(room => {
                      const isChecked = draft.telegramChatRoomIds.includes(room.id);
                      return (
                        <label className={cn(
                          "flex cursor-pointer items-center gap-3 px-2 py-1.5 transition-colors duration-150 hover:bg-surface-raised/50 first:rounded-t last:rounded-b",
                          isChecked ? "bg-surface-raised/30" : ""
                        )} key={room.id}>
                          <input
                            checked={isChecked}
                            className="size-4 accent-primary rounded"
                            onChange={() => toggleTelegramRoom(room.id)}
                            type="checkbox"
                          />
                          <span className="text-xs font-medium text-text">{room.name}</span>
                          <span className="ml-auto font-mono text-[10px] text-muted">{room.chatId}</span>
                        </label>
                      );
                    })}
                    {chatRooms.length === 0 ? (
                      <p className="text-xs text-muted italic">
                        No active Telegram channels configured.
                      </p>
                    ) : null}
                  </div>
                </>
              ) : null}
            </div>
          </div>
        </div>
        {!hasDeliveryTarget ? (
          <p className="mt-3 text-xs text-error font-medium flex items-center gap-1.5">
            <span className="inline-block h-1.5 w-1.5 rounded-full bg-error" />
            Please select at least one delivery destination (WebSocket or Telegram channel).
          </p>
        ) : null}
      </div>

      <div className="flex justify-end gap-2 border-t border-border p-4">
        <button className={managementButtonClass} onClick={onReset} type="button">
          Discard
        </button>
        <button
          className={managementPrimaryButtonClass}
          disabled={
            saving ||
            !draft.applicationId ||
            !draft.name.trim() ||
            !hasDeliveryTarget ||
            activeWindowInvalid
          }
          type="submit"
        >
          {saving ? "Saving..." : editingRule ? "Save changes" : "Create rule"}
        </button>
      </div>
    </form>
  );
}
