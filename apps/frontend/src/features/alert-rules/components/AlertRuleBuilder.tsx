import type { FormEvent } from "react";
import {
  managementButtonClass,
  managementInputClass,
  managementPanelClass,
  managementPrimaryButtonClass
} from "@/shared/components/management-ui";
import type {
  AlertChannelType,
  AlertMetric,
  AlertOperator,
  AlertRule,
  AlertRuleDraft,
  AlertSeverity
} from "../alert-rules-types";

const applicationOptions = ["Payment Gateway", "Auth Service", "Database Node"];
const severityOptions: AlertSeverity[] = ["CRITICAL", "ERROR", "WARN"];
const metricOptions: AlertMetric[] = ["LOG_COUNT", "LATENCY_P95", "DISK_USAGE"];
const operatorOptions: AlertOperator[] = [">", ">=", "<"];
const windowOptions = [30, 60, 300, 600];
const channelOptions: AlertChannelType[] = ["Telegram", "Email", "Webhook"];

type AlertRuleBuilderProps = {
  draft: AlertRuleDraft;
  editingRule: AlertRule | null;
  saving: boolean;
  onDraftChange: (draft: AlertRuleDraft) => void;
  onReset: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export default function AlertRuleBuilder({
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

  return (
    <form className={managementPanelClass} onSubmit={onSubmit}>
      <div className="flex flex-col gap-3 border-b border-border bg-surface-raised/35 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-base font-semibold text-text">Rule Builder</h2>
          <p className="mt-1 text-sm text-muted">
            Define automated alert logic for applications and services.
          </p>
        </div>
        <span className="rounded-md border border-border bg-background px-2 py-1 text-xs font-semibold uppercase text-muted">
          {editingRule ? "Editing rule" : "New draft"}
        </span>
      </div>

      <div className="grid gap-4 p-4 xl:grid-cols-[1.1fr_1fr_1.4fr_1fr]">
        <label className="block">
          <span className="text-[11px] font-semibold uppercase text-muted">
            1. Select application
          </span>
          <select
            className={`mt-2 ${managementInputClass}`}
            onChange={event => updateDraft({ applicationName: event.target.value })}
            value={draft.applicationName}
          >
            {applicationOptions.map(application => (
              <option key={application} value={application}>
                {application}
              </option>
            ))}
          </select>
        </label>

        <label className="block">
          <span className="text-[11px] font-semibold uppercase text-muted">
            2. Severity level
          </span>
          <select
            className={`mt-2 ${managementInputClass}`}
            onChange={event =>
              updateDraft({ severity: event.target.value as AlertSeverity })
            }
            value={draft.severity}
          >
            {severityOptions.map(severity => (
              <option key={severity} value={severity}>
                {severity[0] + severity.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>

        <div>
          <span className="text-[11px] font-semibold uppercase text-muted">
            3. Condition logic
          </span>
          <div className="mt-2 grid grid-cols-[1fr_auto_5rem_auto_5rem] gap-2">
            <select
              aria-label="Metric"
              className={managementInputClass}
              onChange={event =>
                updateDraft({ metric: event.target.value as AlertMetric })
              }
              value={draft.metric}
            >
              {metricOptions.map(metric => (
                <option key={metric} value={metric}>
                  {metric}
                </option>
              ))}
            </select>
            <select
              aria-label="Operator"
              className={managementInputClass}
              onChange={event =>
                updateDraft({ operator: event.target.value as AlertOperator })
              }
              value={draft.operator}
            >
              {operatorOptions.map(operator => (
                <option key={operator} value={operator}>
                  {operator}
                </option>
              ))}
            </select>
            <input
              aria-label="Threshold"
              className={managementInputClass}
              min={1}
              onChange={event =>
                updateDraft({ threshold: Number(event.target.value) })
              }
              type="number"
              value={draft.threshold}
            />
            <span className="self-center text-xs text-muted">per</span>
            <select
              aria-label="Window"
              className={managementInputClass}
              onChange={event =>
                updateDraft({ windowSeconds: Number(event.target.value) })
              }
              value={draft.windowSeconds}
            >
              {windowOptions.map(window => (
                <option key={window} value={window}>
                  {window < 60 ? `${window}s` : `${window / 60}min`}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <span className="text-[11px] font-semibold uppercase text-muted">
            4. Channel
          </span>
          <div className="mt-2 grid gap-2 sm:grid-cols-[8rem_1fr] xl:grid-cols-1">
            <select
              aria-label="Channel type"
              className={managementInputClass}
              onChange={event =>
                updateDraft({ channelType: event.target.value as AlertChannelType })
              }
              value={draft.channelType}
            >
              {channelOptions.map(channel => (
                <option key={channel} value={channel}>
                  {channel}
                </option>
              ))}
            </select>
            <input
              aria-label="Channel target"
              className={managementInputClass}
              onChange={event => updateDraft({ channelTarget: event.target.value })}
              required
              value={draft.channelTarget}
            />
          </div>
        </div>
      </div>

      <div className="grid gap-4 border-t border-border bg-background/30 p-4 lg:grid-cols-[1fr_16rem]">
        <label className="block">
          <span className="text-sm font-medium text-text">Rule name</span>
          <input
            className={`mt-2 ${managementInputClass}`}
            onChange={event => updateDraft({ name: event.target.value })}
            required
            value={draft.name}
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-text">Service boundary</span>
          <input
            className={`mt-2 ${managementInputClass}`}
            onChange={event => updateDraft({ serviceName: event.target.value })}
            required
            value={draft.serviceName}
          />
        </label>
      </div>

      <div className="flex justify-end gap-2 border-t border-border p-4">
        <button className={managementButtonClass} onClick={onReset} type="button">
          Discard
        </button>
        <button
          className={managementPrimaryButtonClass}
          disabled={saving}
          type="submit"
        >
          {editingRule ? "Save Rule" : "Create Rule"}
        </button>
      </div>
    </form>
  );
}
