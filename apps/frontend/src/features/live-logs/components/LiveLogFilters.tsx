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
    <section className="rounded-lg border border-border bg-surface px-3 py-2">
      <div className="grid items-end gap-2 md:grid-cols-[minmax(12rem,1.1fr)_10rem_minmax(14rem,1fr)_8rem]">
        <label className="space-y-1">
          <span className="text-xs font-medium uppercase text-muted">
            Application
          </span>
          <select
            aria-label="Application"
            className="min-h-9 w-full rounded-md border border-border bg-background px-2.5 text-sm text-text outline-none transition focus:border-primary"
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
        <label className="space-y-1">
          <span className="text-xs font-medium uppercase text-muted">Level</span>
          <select
            aria-label="Level"
            className="min-h-9 w-full rounded-md border border-border bg-background px-2.5 text-sm text-text outline-none transition focus:border-primary"
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
        <label className="space-y-1">
          <span className="text-xs font-medium uppercase text-muted">
            Keyword
          </span>
          <input
            aria-label="Keyword"
            className="min-h-9 w-full rounded-md border border-border bg-background px-2.5 text-sm text-text outline-none transition focus:border-primary"
            onChange={(event) =>
              onChange({ ...filters, keyword: event.target.value })
            }
            placeholder="timeout, unauthorized..."
            value={filters.keyword}
          />
        </label>
        <div>
          <button
            className="inline-flex min-h-9 w-full items-center justify-center rounded-md border border-border bg-surface-raised px-3 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
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
