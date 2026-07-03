import { useEffect, useState } from "react";
import {
  changeUserRole,
  changeUserStatus,
  getUser,
  getUserApplicationAccess,
  getUserAccessError,
  softDeleteUser
} from "@/features/user-access/user-access-api";
import type {
  ApplicationAccess,
  User,
  UserRole,
  UserStatus
} from "@/features/user-access/user-access-types";
import ConfirmDialog from "./ConfirmDialog";
import DialogShell from "./DialogShell";
import UserApplicationAccessList from "./UserApplicationAccessList";
import UserIdentityPanel from "./UserIdentityPanel";
import UserRoleStatusPanel from "./UserRoleStatusPanel";

type UserDetailsDialogProps = {
  user: User;
  onClose: () => void;
  onUpdated: (user: User) => void;
  onManageAccess: (user: User) => void;
  onEdit: (user: User) => void;
  onDeleted: (user: User) => void;
};

export default function UserDetailsDialog({
  user,
  onClose,
  onUpdated,
  onManageAccess,
  onEdit,
  onDeleted
}: UserDetailsDialogProps) {
  const [detail, setDetail] = useState(user);
  const [role, setRole] = useState<UserRole>(
    user.role === "ADMIN" ? "ADMIN" : "ENGINEER"
  );
  const [loading, setLoading] = useState(Boolean(user.id));
  const [accessLoading, setAccessLoading] = useState(Boolean(user.id));
  const [accesses, setAccesses] = useState<ApplicationAccess[]>([]);
  const [accessError, setAccessError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [confirmRoleDowngrade, setConfirmRoleDowngrade] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    if (!user.id) {
      return;
    }

    let active = true;
    getUser(user.id)
      .then(nextUser => {
        if (active) {
          setDetail(nextUser);
          setRole(nextUser.role === "ADMIN" ? "ADMIN" : "ENGINEER");
        }
      })
      .catch(loadError => {
        if (active) {
          setError(getUserAccessError(loadError, "Unable to load user details."));
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [user.id]);

  useEffect(() => {
    if (!user.id) {
      return;
    }

    let active = true;
    getUserApplicationAccess(user.id)
      .then(nextAccesses => {
        if (active) {
          setAccesses(nextAccesses);
        }
      })
      .catch(loadError => {
        if (active) {
          setAccessError(
            getUserAccessError(
              loadError,
              "Unable to load this user's application access."
            )
          );
        }
      })
      .finally(() => {
        if (active) {
          setAccessLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, [user.id]);

  async function performSaveRole() {
    if (!detail.id || role === detail.role) {
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const updated = await changeUserRole(detail.id, role);
      setDetail(updated);
      onUpdated(updated);
    } catch (saveError) {
      setError(getUserAccessError(saveError, "Unable to change user role."));
    } finally {
      setSaving(false);
    }
  }

  function saveRole() {
    if (detail.role === "ADMIN" && role === "ENGINEER") {
      setConfirmRoleDowngrade(true);
      return;
    }

    void performSaveRole();
  }

  async function setStatus(status: UserStatus) {
    if (!detail.id || status === detail.status) {
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const updated = await changeUserStatus(detail.id, status);
      setDetail(updated);
      onUpdated(updated);
    } catch (saveError) {
      setError(getUserAccessError(saveError, "Unable to change user status."));
    } finally {
      setSaving(false);
    }
  }

  async function deleteUser() {
    if (!detail.id) {
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const deleted = await softDeleteUser(detail.id);
      onDeleted(deleted);
      onClose();
    } catch (deleteError) {
      setError(getUserAccessError(deleteError, "Unable to delete user."));
      setConfirmDelete(false);
    } finally {
      setSaving(false);
    }
  }

  const isDeleted = detail.status === "DELETED";

  return (
    <DialogShell
      description={detail.email}
      eyebrow="User details"
      footer={
        <>
          <button
            className="min-h-10 rounded-md border border-error/40 px-4 text-sm font-medium text-error transition hover:bg-error/10 disabled:opacity-40"
            disabled={saving || isDeleted}
            onClick={() => setConfirmDelete(true)}
            type="button"
          >
            Delete user
          </button>
          <button
            className="min-h-10 rounded-md border border-border px-4 text-sm font-medium text-text transition hover:bg-surface-raised disabled:opacity-40"
            disabled={saving || isDeleted}
            onClick={() => onEdit(detail)}
            type="button"
          >
            Edit user
          </button>
          <button
            className="min-h-10 rounded-md border border-border px-4 text-sm font-medium text-text transition hover:bg-surface-raised"
            onClick={onClose}
            type="button"
          >
            Close
          </button>
          <button
            className="min-h-10 rounded-md bg-primary px-4 text-sm font-semibold text-primary-foreground transition hover:bg-primary-hover"
            disabled={isDeleted}
            onClick={() => onManageAccess(detail)}
            type="button"
          >
            Manage application access
          </button>
        </>
      }
      onClose={onClose}
      size="xl"
      title={detail.displayName || "User details"}
    >
      {loading ? <p className="text-sm text-muted">Loading user details...</p> : null}

      {!loading ? (
        <div className="space-y-5">
          <UserIdentityPanel user={detail} />

          <UserRoleStatusPanel
            isDeleted={isDeleted}
            onRoleChange={setRole}
            onSaveRole={saveRole}
            onSetStatus={status => void setStatus(status)}
            role={role}
            saving={saving}
            user={detail}
          />

          <UserApplicationAccessList
            accesses={accesses}
            error={accessError}
            loading={accessLoading}
          />

          {error ? (
            <p className="rounded-md border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
              {error}
            </p>
          ) : null}
        </div>
      ) : null}

      {confirmRoleDowngrade ? (
        <ConfirmDialog
          confirmLabel="Downgrade to engineer"
          description="This user will lose administrator permissions and will no longer be able to manage users or platform-wide access."
          loading={saving}
          onCancel={() => setConfirmRoleDowngrade(false)}
          onConfirm={() => {
            setConfirmRoleDowngrade(false);
            void performSaveRole();
          }}
          title="Downgrade administrator?"
          tone="warning"
        />
      ) : null}

      {confirmDelete ? (
        <ConfirmDialog
          confirmLabel="Delete user"
          description="This is a soft delete. The account will be marked as deleted and hidden from the default user list."
          loading={saving}
          onCancel={() => setConfirmDelete(false)}
          onConfirm={() => void deleteUser()}
          title="Delete this user?"
          tone="danger"
        />
      ) : null}
    </DialogShell>
  );
}
