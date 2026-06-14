import { managementPanelClass } from "@/shared/components/management-ui";
import type { UserResponse } from "../profile-api";
import { getRoleDescription, getStatusDescription } from "./profile-ui";

export default function ProfileAccessSummary({
  profile
}: {
  profile: UserResponse;
}) {
  return (
    <aside className="space-y-5">
      <section className={`${managementPanelClass} p-4`}>
        <h3 className="text-base font-semibold text-text">Access summary</h3>
        <div className="mt-4 space-y-3">
          <div className="rounded-md border border-border bg-surface-raised p-3">
            <p className="text-xs uppercase tracking-wide text-muted">Role</p>
            <p className="mt-2 text-sm font-medium text-text">
              {profile.role || "Unknown role"}
            </p>
            <p className="mt-2 text-sm text-muted">
              {getRoleDescription(profile.role)}
            </p>
          </div>
          <div className="rounded-md border border-border bg-surface-raised p-3">
            <p className="text-xs uppercase tracking-wide text-muted">Status</p>
            <p className="mt-2 text-sm font-medium text-text">
              {profile.status || "Unknown status"}
            </p>
            <p className="mt-2 text-sm text-muted">
              {getStatusDescription(profile.status)}
            </p>
          </div>
        </div>
      </section>
    </aside>
  );
}
