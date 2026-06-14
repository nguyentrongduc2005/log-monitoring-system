import { StatusBadge } from "@/shared/components/management-ui";
import type { UserResponse } from "../profile-api";
import { formatDate, statusTone } from "./profile-ui";

export default function ProfileIdentityCard({
  profile
}: {
  profile: UserResponse;
}) {
  return (
    <section className="rounded-lg border border-border bg-surface p-4">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-muted">
            Account
          </p>
          <h2 className="mt-2 text-xl font-semibold text-text">
            {profile.displayName || "Unknown user"}
          </h2>
          <p className="mt-1 text-sm text-muted">
            {profile.email || "No email available"}
          </p>
        </div>
        <div className="flex flex-wrap gap-2 text-xs font-medium">
          <StatusBadge tone="primary">{profile.role || "Unknown role"}</StatusBadge>
          <StatusBadge tone={statusTone(profile.status)}>
            {profile.status || "Unknown status"}
          </StatusBadge>
        </div>
      </div>
      <dl className="mt-5 grid gap-3 sm:grid-cols-2">
        <div className="rounded-md border border-border bg-surface-raised p-3">
          <dt className="text-xs uppercase tracking-wide text-muted">User ID</dt>
          <dd className="mt-2 break-all font-mono text-sm text-text">
            {profile.id || "Not available"}
          </dd>
        </div>
        <div className="rounded-md border border-border bg-surface-raised p-3">
          <dt className="text-xs uppercase tracking-wide text-muted">Last login</dt>
          <dd className="mt-2 text-sm text-text">
            {formatDate(profile.lastLoginAt)}
          </dd>
        </div>
        <div className="rounded-md border border-border bg-surface-raised p-3">
          <dt className="text-xs uppercase tracking-wide text-muted">Created</dt>
          <dd className="mt-2 text-sm text-text">{formatDate(profile.createdAt)}</dd>
        </div>
        <div className="rounded-md border border-border bg-surface-raised p-3">
          <dt className="text-xs uppercase tracking-wide text-muted">Updated</dt>
          <dd className="mt-2 text-sm text-text">{formatDate(profile.updatedAt)}</dd>
        </div>
      </dl>
    </section>
  );
}

