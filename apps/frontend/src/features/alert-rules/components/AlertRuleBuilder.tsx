import type { FormEvent } from "react";
import type { Application } from "@/features/applications/application-types";
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

      <div className="grid gap-4 p-4 lg:grid-cols-2 xl:grid-cols-3">
        <label className="block">
          <span className="text-sm font-medium text-text">Application</span>
          <select
            className={`mt-2 ${managementInputClass}`}
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
            <span className="mt-1 block text-xs text-muted">
              Application cannot be changed after creation.
            </span>
          ) : null}
        </label>

        <label className="block">
          <span className="text-sm font-medium text-text">Rule name</span>
          <input
            className={`mt-2 ${managementInputClass}`}
            maxLength={120}
            onChange={event => updateDraft({ name: event.target.value })}
            placeholder="Critical payment errors"
            required
            value={draft.name}
          />
        </label>

        <label className="block">
          <span className="text-sm font-medium text-text">Minimum severity</span>
          <select
            className={`mt-2 ${managementInputClass}`}
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

        <label className="block lg:col-span-2 xl:col-span-1">
          <span className="text-sm font-medium text-text">Keyword</span>
          <input
            className={`mt-2 ${managementInputClass}`}
            maxLength={255}
            onChange={event => updateDraft({ keywordPattern: event.target.value })}
            placeholder="Optional text contained in the log message"
            value={draft.keywordPattern}
          />
        </label>

        <label className="block">
          <span className="text-sm font-medium text-text">Threshold count</span>
          <input
            className={`mt-2 ${managementInputClass}`}
            min={1}
            onChange={event => updateDraft({ thresholdCount: Number(event.target.value) })}
            required
            type="number"
            value={draft.thresholdCount}
          />
        </label>

        <label className="block">
          <span className="text-sm font-medium text-text">Window (seconds)</span>
          <input
            className={`mt-2 ${managementInputClass}`}
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
          <span className="text-sm font-medium text-text">Cooldown (seconds)</span>
          <input
            className={`mt-2 ${managementInputClass}`}
            min={1}
            onChange={event => updateDraft({ cooldownSeconds: Number(event.target.value) })}
            required
            type="number"
            value={draft.cooldownSeconds}
          />
        </label>

        <label className="block lg:col-span-2">
          <span className="text-sm font-medium text-text">Description</span>
          <textarea
            className={`mt-2 min-h-24 ${managementInputClass}`}
            maxLength={2000}
            onChange={event => updateDraft({ description: event.target.value })}
            placeholder="Optional context for operators"
            value={draft.description}
          />
        </label>
      </div>

      <fieldset className="border-t border-border p-4">
        <legend className="px-1 text-sm font-semibold text-text">
          Notification destinations
        </legend>
        <div className="mt-2 grid gap-4 lg:grid-cols-2">
          <label className="flex cursor-pointer items-start gap-3 rounded-lg border border-border bg-background p-4">
            <input
              checked={draft.websocketEnabled}
              className="mt-1 size-4 accent-primary"
              onChange={event => updateDraft({ websocketEnabled: event.target.checked })}
              type="checkbox"
            />
            <span>
              <span className="block font-medium text-text">WebSocket</span>
              <span className="mt-1 block text-xs text-muted">
                Publish to connected dashboard clients. No chat room required.
              </span>
            </span>
          </label>

          <div className="rounded-lg border border-border bg-background p-4">
            <p className="font-medium text-text">Telegram chat rooms</p>
            <p className="mt-1 text-xs text-muted">
              Select any number of active rooms.
            </p>
            <div className="mt-3 max-h-40 space-y-2 overflow-y-auto">
              {chatRooms.map(room => (
                <label className="flex cursor-pointer items-center gap-3" key={room.id}>
                  <input
                    checked={draft.telegramChatRoomIds.includes(room.id)}
                    className="size-4 accent-primary"
                    onChange={() => toggleTelegramRoom(room.id)}
                    type="checkbox"
                  />
                  <span className="text-sm text-text">{room.name}</span>
                  <span className="ml-auto text-xs text-muted">{room.chatId}</span>
                </label>
              ))}
              {chatRooms.length === 0 ? (
                <p className="text-sm text-muted">
                  No active Telegram rooms. Create one in Notification Channels.
                </p>
              ) : null}
            </div>
          </div>
        </div>
        {!hasDeliveryTarget ? (
          <p className="mt-3 text-sm text-warning">
            Select WebSocket or at least one Telegram chat room.
          </p>
        ) : null}
      </fieldset>

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
            !hasDeliveryTarget
          }
          type="submit"
        >
          {saving ? "Saving..." : editingRule ? "Save changes" : "Create rule"}
        </button>
      </div>
    </form>
  );
}
