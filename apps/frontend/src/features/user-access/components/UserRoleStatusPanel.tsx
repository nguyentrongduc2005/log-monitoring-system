import type { User, UserRole, UserStatus } from "../user-access-types";

type UserRoleStatusPanelProps = {
  isDeleted: boolean;
  role: UserRole;
  saving: boolean;
  user: User;
  onRoleChange: (role: UserRole) => void;
  onSaveRole: () => void;
  onSetStatus: (status: UserStatus) => void;
};

export default function UserRoleStatusPanel({
  isDeleted,
  role,
  saving,
  user,
  onRoleChange,
  onSaveRole,
  onSetStatus
}: UserRoleStatusPanelProps) {
  return (
    <section className="grid gap-4 lg:grid-cols-2">
      <div className="rounded-lg border border-border bg-background p-4">
        <h3 className="text-base font-semibold text-text">System role</h3>
        <p className="mt-1 text-sm text-muted">
          Defines the user&apos;s platform-wide permissions.
        </p>
        <div className="mt-4 flex gap-3">
          <select
            aria-label="User role"
            className="min-h-10 min-w-0 flex-1 rounded-md border border-border bg-surface px-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            disabled={saving || isDeleted}
            onChange={event => onRoleChange(event.target.value as UserRole)}
            value={role}
          >
            <option value="ENGINEER">Engineer</option>
            <option value="ADMIN">Administrator</option>
          </select>
          <button
            aria-label="Save role"
            className="min-h-10 rounded-md bg-primary px-4 text-sm font-semibold text-primary-foreground transition hover:bg-primary-hover disabled:opacity-50"
            disabled={saving || isDeleted || role === user.role}
            onClick={onSaveRole}
            type="button"
          >
            Save
          </button>
        </div>
      </div>

      <div className="rounded-lg border border-border bg-background p-4">
        <h3 className="text-base font-semibold text-text">Account status</h3>
        <p className="mt-1 text-sm text-muted">
          Control whether this user can sign in.
        </p>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            className="min-h-10 flex-1 rounded-md border border-success/40 bg-success/5 px-3 text-sm font-medium text-success transition hover:bg-success/10 disabled:opacity-40"
            disabled={saving || isDeleted || user.status === "ACTIVE"}
            onClick={() => onSetStatus("ACTIVE")}
            type="button"
          >
            Activate
          </button>
          <button
            className="min-h-10 flex-1 rounded-md border border-error/40 bg-error/5 px-3 text-sm font-medium text-error transition hover:bg-error/10 disabled:opacity-40"
            disabled={saving || isDeleted || user.status === "DISABLED"}
            onClick={() => onSetStatus("DISABLED")}
            type="button"
          >
            Disable
          </button>
          <button
            className="min-h-10 flex-1 rounded-md border border-warning/40 bg-warning/5 px-3 text-sm font-medium text-warning transition hover:bg-warning/10 disabled:opacity-40"
            disabled={saving || isDeleted || user.status === "LOCKED"}
            onClick={() => onSetStatus("LOCKED")}
            type="button"
          >
            Lock
          </button>
        </div>
      </div>
    </section>
  );
}

