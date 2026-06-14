import {
  managementButtonClass,
  managementPanelClass,
  managementPrimaryButtonClass,
  StatusBadge
} from "@/shared/components/management-ui";
import type {
  RetentionAction,
  RetentionJob,
  RetentionJobDraft
} from "../retention-types";
import { actionLabel, logLevelTone } from "./retention-ui";

const actionOptions: RetentionAction[] = ["DELETE", "COMPRESS", "ARCHIVE"];

type RetentionControlsProps = {
  drafts: RetentionJobDraft[];
  error: string | null;
  jobs: RetentionJob[];
  loading: boolean;
  saving: boolean;
  onReset: () => void;
  onSave: () => void;
  onUpdateDraft: (draft: RetentionJobDraft) => void;
};

export default function RetentionControls({
  drafts,
  error,
  jobs,
  loading,
  saving,
  onReset,
  onSave,
  onUpdateDraft
}: RetentionControlsProps) {
  const draftById = new Map(drafts.map(draft => [draft.id, draft]));

  return (
    <section className={managementPanelClass}>
      <div className="flex flex-col gap-3 border-b border-border bg-surface-raised/35 p-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="text-base font-semibold text-text">Log Aging Controls</h2>
          <p className="mt-1 text-sm text-muted">
            Configure the three default ClickHouse retention jobs by log level.
          </p>
        </div>
        <div className="flex gap-2">
          <button
            className={managementPrimaryButtonClass}
            disabled={loading || saving}
            onClick={onSave}
            type="button"
          >
            Save Changes
          </button>
          <button
            className={managementButtonClass}
            disabled={loading || saving}
            onClick={onReset}
            type="button"
          >
            Reset
          </button>
        </div>
      </div>

      {error ? (
        <p className="m-4 rounded-md border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
          {error}
        </p>
      ) : null}

      {loading ? (
        <div className="p-4 text-sm text-muted">Loading retention jobs...</div>
      ) : (
        <div className="divide-y divide-border">
          {jobs.map(job => {
            const draft = draftById.get(job.id) ?? {
              id: job.id,
              action: job.action,
              retentionDays: job.retentionDays
            };

            return (
              <div className="p-4" key={job.id}>
                <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <StatusBadge tone={logLevelTone(job.logLevel)}>
                        {job.label}
                      </StatusBadge>
                      <span className="text-xs text-muted">{job.description}</span>
                    </div>
                  </div>
                  <span className="rounded-md bg-surface-raised px-2 py-1 text-xs font-semibold text-text ring-1 ring-border">
                    {draft.retentionDays} Days
                  </span>
                </div>

                <label className="mt-4 block">
                  <span className="sr-only">{job.label} retention days</span>
                  <input
                    aria-label={`${job.label} retention days`}
                    className="h-2 w-full accent-primary"
                    max={job.maxDays}
                    min={job.minDays}
                    onChange={event =>
                      onUpdateDraft({
                        ...draft,
                        retentionDays: Number(event.target.value)
                      })
                    }
                    type="range"
                    value={draft.retentionDays}
                  />
                </label>
                <div className="mt-1 flex justify-between text-[11px] text-muted">
                  <span>{job.minDays}d</span>
                  <span>{Math.round((job.minDays + job.maxDays) / 2)}d</span>
                  <span>{job.maxDays}d</span>
                </div>

                <div className="mt-3 grid gap-2 sm:grid-cols-3">
                  {actionOptions.map(action => {
                    const selected = draft.action === action;
                    return (
                      <label
                        className={`flex min-h-10 items-center gap-2 rounded-md border px-3 text-sm transition ${
                          selected
                            ? "border-primary bg-primary/10 text-primary"
                            : "border-border bg-background text-text hover:border-primary/60"
                        }`}
                        key={action}
                      >
                        <input
                          checked={selected}
                          className="accent-primary"
                          name={`${job.id}-action`}
                          onChange={() => onUpdateDraft({ ...draft, action })}
                          type="radio"
                        />
                        {actionLabel(action)}
                      </label>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
