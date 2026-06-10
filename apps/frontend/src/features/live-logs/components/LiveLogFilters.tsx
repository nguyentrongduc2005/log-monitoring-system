import type {
  ApplicationOption,
  LiveLogFilters as LiveLogFiltersValue
} from "@/features/live-logs/live-logs-types";

type LiveLogFiltersProps = {
  applications: ApplicationOption[];
  filters: LiveLogFiltersValue;
  onChange: (filters: LiveLogFiltersValue) => void;
  onReset: () => void;
};

export default function LiveLogFilters({
  applications,
  filters,
  onChange,
  onReset
}: LiveLogFiltersProps) {
  return (
    <section className="rounded-2xl border border-border bg-surface p-5">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <label className="space-y-2">
          <span className="text-sm font-medium text-text">Application</span>
          <select
            aria-label="Application"
            className="min-h-11 w-full rounded-xl border border-border bg-surface-raised px-3 text-sm text-text outline-none transition focus:border-primary"
            onChange={(event) =>
              onChange({ ...filters, applicationId: event.target.value })
            }
            value={filters.applicationId}
          >
            <option value="">All applications</option>
            {applications.map((application) => (
              <option key={application.id} value={application.id}>
                {application.name}
              </option>
            ))}
          </select>
        </label>
        <label className="space-y-2">
          <span className="text-sm font-medium text-text">Level</span>
          <select
            aria-label="Level"
            className="min-h-11 w-full rounded-xl border border-border bg-surface-raised px-3 text-sm text-text outline-none transition focus:border-primary"
            onChange={(event) =>
              onChange({
                ...filters,
                level: event.target.value as LiveLogFiltersValue["level"]
              })
            }
            value={filters.level}
          >
            <option value="ALL">All levels</option>
            <option value="INFO">INFO</option>
            <option value="WARN">WARN</option>
            <option value="ERROR">ERROR</option>
            <option value="CRITICAL">CRITICAL</option>
          </select>
        </label>
        <label className="space-y-2">
          <span className="text-sm font-medium text-text">Keyword</span>
          <input
            aria-label="Keyword"
            className="min-h-11 w-full rounded-xl border border-border bg-surface-raised px-3 text-sm text-text outline-none transition focus:border-primary"
            onChange={(event) =>
              onChange({ ...filters, keyword: event.target.value })
            }
            value={filters.keyword}
          />
        </label>
        <label className="space-y-2">
          <span className="text-sm font-medium text-text">Trace ID</span>
          <input
            aria-label="Trace ID"
            className="min-h-11 w-full rounded-xl border border-border bg-surface-raised px-3 text-sm text-text outline-none transition focus:border-primary"
            onChange={(event) =>
              onChange({ ...filters, traceId: event.target.value })
            }
            value={filters.traceId}
          />
        </label>
        <div className="flex items-end">
          <button
            className="inline-flex min-h-11 w-full items-center justify-center rounded-xl border border-border bg-surface-raised px-4 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={onReset}
            type="button"
          >
            Reset filters
          </button>
        </div>
      </div>
    </section>
  );
}
