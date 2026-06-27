import type { User } from "@/features/user-access/user-access-types";

type UserTableProps = {
  users: User[];
  onView: (user: User) => void;
  onManageAccess: (user: User) => void;
};

function formatDate(value?: string) {
  if (!value) {
    return "Never";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Unknown";
  }

  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

function badgeClasses(value?: string) {
  if (value === "ACTIVE") {
    return "bg-success/15 text-success";
  }
  if (value === "LOCKED") {
    return "bg-warning/15 text-warning";
  }
  if (value === "ADMIN") {
    return "bg-primary/15 text-primary";
  }
  if (value === "DELETED") {
    return "bg-error/15 text-error";
  }
  return "bg-surface-raised text-muted";
}

export default function UserTable({
  users,
  onView,
  onManageAccess
}: UserTableProps) {
  return (
    <section className="overflow-hidden rounded-lg border border-border bg-surface">
      <div className="shell-scrollbar overflow-x-auto">
        <table className="w-full min-w-[760px] text-left text-sm">
          <thead className="border-b border-border bg-surface-raised/60 text-xs uppercase tracking-wide text-muted">
            <tr>
              <th className="px-5 py-3 font-medium">User</th>
              <th className="px-5 py-3 font-medium">Role</th>
              <th className="px-5 py-3 font-medium">Status</th>
              <th className="px-5 py-3 font-medium">Last login</th>
              <th className="px-5 py-3 text-right font-medium">Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map(user => (
              <tr
                className="border-b border-border transition last:border-0 hover:bg-surface-raised/40"
                key={user.id ?? user.email}
              >
                <td className="px-5 py-4">
                  <p className="font-medium text-text">
                    {user.displayName || "Unnamed user"}
                  </p>
                  <p className="mt-0.5 text-xs text-muted">{user.email}</p>
                </td>
                <td className="px-5 py-4">
                  <span
                    className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${badgeClasses(user.role)}`}
                  >
                    {user.role || "UNKNOWN"}
                  </span>
                </td>
                <td className="px-5 py-4">
                  <span
                    className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${badgeClasses(user.status)}`}
                  >
                    {user.status || "UNKNOWN"}
                  </span>
                </td>
                <td className="px-5 py-4 text-muted">
                  {formatDate(user.lastLoginAt)}
                </td>
                <td className="px-5 py-4">
                  <div className="flex justify-end gap-2">
                    <button
                      className="rounded-lg border border-border px-3 py-2 text-xs font-medium text-text transition hover:border-primary hover:text-primary"
                      onClick={() => onView(user)}
                      type="button"
                    >
                      View details
                    </button>
                    <button
                      className="rounded-lg bg-primary/15 px-3 py-2 text-xs font-medium text-primary transition hover:bg-primary/25"
                      disabled={!user.id || user.status === "DELETED"}
                      onClick={() => onManageAccess(user)}
                      type="button"
                    >
                      Manage access
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {users.length === 0 ? (
        <div className="px-5 py-12 text-center">
          <p className="font-medium text-text">No users found</p>
          <p className="mt-1 text-sm text-muted">
            Try another search or create a new account.
          </p>
        </div>
      ) : null}
    </section>
  );
}
