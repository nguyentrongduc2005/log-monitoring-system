import type { User } from "../user-access-types";
import { formatDate, initials, statusClasses } from "./user-details-ui";

export default function UserIdentityPanel({ user }: { user: User }) {
  return (
    <section className="flex flex-col gap-4 rounded-lg border border-border bg-background p-4 sm:flex-row sm:items-center">
      <div className="grid size-12 shrink-0 place-items-center rounded-md bg-primary/15 text-lg font-semibold text-primary">
        {initials(user.displayName || user.email)}
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="text-base font-semibold text-text">
            {user.displayName || "Unnamed user"}
          </h3>
          <span
            className={`inline-flex rounded-md px-2 py-1 text-xs font-semibold ${statusClasses(user.status)}`}
          >
            {user.status}
          </span>
          <span className="inline-flex rounded-md bg-primary/10 px-2 py-1 text-xs font-semibold text-primary">
            {user.role}
          </span>
        </div>
        <p className="mt-1 text-sm text-muted">{user.email}</p>
        <p className="mt-2 break-all font-mono text-xs text-muted">{user.id}</p>
      </div>
      <div className="grid grid-cols-2 gap-4 border-t border-border pt-4 sm:border-l sm:border-t-0 sm:pl-4 sm:pt-0">
        <div>
          <p className="text-xs uppercase tracking-wide text-muted">Created</p>
          <p className="mt-1 text-sm text-text">{formatDate(user.createdAt)}</p>
        </div>
        <div>
          <p className="text-xs uppercase tracking-wide text-muted">Last login</p>
          <p className="mt-1 text-sm text-text">{formatDate(user.lastLoginAt)}</p>
        </div>
      </div>
    </section>
  );
}

