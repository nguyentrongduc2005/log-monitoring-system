import type { ApplicationAccess } from "../user-access-types";

type UserApplicationAccessListProps = {
  accesses: ApplicationAccess[];
  error: string | null;
  loading: boolean;
};

export default function UserApplicationAccessList({
  accesses,
  error,
  loading
}: UserApplicationAccessListProps) {
  return (
    <section className="rounded-lg border border-border bg-background p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="font-semibold text-text">Application access</h3>
          <p className="mt-1 text-sm text-muted">
            Applications this user can currently view or manage.
          </p>
        </div>
        <span className="rounded-md bg-surface-raised px-2 py-1 text-xs font-semibold text-muted">
          {accesses.length} assigned
        </span>
      </div>

      {loading ? (
        <p className="mt-4 text-sm text-muted">Loading application access...</p>
      ) : null}

      {!loading && accesses.length > 0 ? (
        <div className="mt-4 overflow-hidden rounded-lg border border-border bg-surface">
          {accesses.map(access => (
            <div
              className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-4 py-3 last:border-0"
              key={access.applicationId}
            >
              <div>
                <p className="text-sm font-medium text-text">
                  {access.applicationDisplayName ||
                    access.applicationName ||
                    "Unknown application"}
                </p>
                <p className="mt-0.5 text-xs text-muted">
                  {access.applicationName || access.applicationId}
                </p>
              </div>
              <span
                className={`rounded-md px-2 py-1 text-xs font-semibold ${
                  access.accessLevel === "MANAGE"
                    ? "bg-primary/15 text-primary"
                    : "bg-success/15 text-success"
                }`}
              >
                {access.accessLevel || "VIEW"}
              </span>
            </div>
          ))}
        </div>
      ) : null}

      {!loading && !error && accesses.length === 0 ? (
        <p className="mt-4 rounded-md border border-dashed border-border px-4 py-5 text-center text-sm text-muted">
          No application access has been granted to this user.
        </p>
      ) : null}

      {error ? (
        <p className="mt-4 rounded-md border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
          {error}
        </p>
      ) : null}
    </section>
  );
}
