import { useCallback, useEffect, useState } from "react";
import { PageHeader } from "@/shared/layouts/page-header-context";
import {
  getUserAccessError,
  getUsers
} from "@/features/user-access/user-access-api";
import type {
  User,
  UserPage,
  UserRole,
  UserStatus
} from "@/features/user-access/user-access-types";
import AccessDialog from "./components/AccessDialog";
import CreateUserDialog from "./components/CreateUserDialog";
import EditUserDialog from "./components/EditUserDialog";
import UserDetailsDialog from "./components/UserDetailsDialog";
import UserTable from "./components/UserTable";

const emptyUserPage: UserPage = {
  users: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0
};

export function Component() {
  const [userPage, setUserPage] = useState<UserPage>(emptyUserPage);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<UserRole | "">("");
  const [statusFilter, setStatusFilter] = useState<UserStatus | "">("");
  const [includeDeleted, setIncludeDeleted] = useState(false);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [detailUser, setDetailUser] = useState<User | null>(null);
  const [editUser, setEditUser] = useState<User | null>(null);
  const [accessUser, setAccessUser] = useState<User | null>(null);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setUserPage(
        await getUsers({
          page,
          size,
          search,
          role: roleFilter,
          status: statusFilter,
          includeDeleted
        })
      );
    } catch (loadError) {
      setError(getUserAccessError(loadError, "Unable to load users."));
    } finally {
      setLoading(false);
    }
  }, [includeDeleted, page, roleFilter, search, size, statusFilter]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadUsers();
    });
  }, [loadUsers]);

  const users = userPage.users;
  const activeUsers = users.filter(user => user.status === "ACTIVE").length;
  const adminUsers = users.filter(user => user.role === "ADMIN").length;
  const currentPage = userPage.totalPages === 0 ? 0 : userPage.page + 1;

  function replaceUser(updated: User) {
    setUserPage(current => ({
      ...current,
      users: current.users.map(user => (user.id === updated.id ? updated : user))
    }));
    setDetailUser(current => (current?.id === updated.id ? updated : current));
    setEditUser(current => (current?.id === updated.id ? updated : current));
  }

  function resetPage() {
    setPage(0);
  }

  return (
    <div className="space-y-6">
      <PageHeader
        actions={
          <div className="flex flex-wrap gap-2">
            <button
              className="rounded-lg border border-border bg-surface-raised px-4 py-2 text-sm font-medium text-text transition hover:border-primary hover:text-primary"
              disabled={loading}
              onClick={() => void loadUsers()}
              type="button"
            >
              Refresh
            </button>
            <button
              className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition hover:bg-primary-hover"
              onClick={() => setShowCreate(true)}
              type="button"
            >
              Create user
            </button>
          </div>
        }
        title="Users & Access"
      />

      <section className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-lg border border-border bg-surface p-4">
          <p className="text-sm text-muted">Total users</p>
          <p className="mt-2 text-2xl font-semibold text-text">
            {userPage.totalElements}
          </p>
        </div>
        <div className="rounded-lg border border-border bg-surface p-4">
          <p className="text-sm text-muted">Active on page</p>
          <p className="mt-2 text-2xl font-semibold text-success">
            {activeUsers}
          </p>
        </div>
        <div className="rounded-lg border border-border bg-surface p-4">
          <p className="text-sm text-muted">Admins on page</p>
          <p className="mt-2 text-2xl font-semibold text-primary">
            {adminUsers}
          </p>
        </div>
      </section>

      <section className="grid gap-4 rounded-lg border border-border bg-surface p-4 lg:grid-cols-[minmax(0,1fr)_180px_180px_160px]">
        <label className="block">
          <span className="text-xs font-medium uppercase tracking-wide text-muted">
            Search
          </span>
          <input
            aria-label="Search users"
            className="mt-2 w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-text outline-none placeholder:text-muted focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => {
              setSearch(event.target.value);
              resetPage();
            }}
            placeholder="Search by name, email, role, or status..."
            value={search}
          />
        </label>
        <label className="block">
          <span className="text-xs font-medium uppercase tracking-wide text-muted">
            Role
          </span>
          <select
            aria-label="Filter by role"
            className="mt-2 min-h-11 w-full rounded-lg border border-border bg-background px-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => {
              setRoleFilter(event.target.value as UserRole | "");
              resetPage();
            }}
            value={roleFilter}
          >
            <option value="">All roles</option>
            <option value="ADMIN">Admin</option>
            <option value="ENGINEER">Engineer</option>
          </select>
        </label>
        <label className="block">
          <span className="text-xs font-medium uppercase tracking-wide text-muted">
            Status
          </span>
          <select
            aria-label="Filter by status"
            className="mt-2 min-h-11 w-full rounded-lg border border-border bg-background px-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => {
              const nextStatus = event.target.value as UserStatus | "";
              setStatusFilter(nextStatus);
              if (nextStatus === "DELETED") {
                setIncludeDeleted(true);
              }
              resetPage();
            }}
            value={statusFilter}
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="DISABLED">Disabled</option>
            <option value="LOCKED">Locked</option>
            <option value="DELETED">Deleted</option>
          </select>
        </label>
        <label className="flex items-end gap-2 pb-3 text-sm text-muted">
          <input
            checked={includeDeleted}
            className="size-4 rounded border-border bg-background text-primary"
            onChange={event => {
              setIncludeDeleted(event.target.checked);
              resetPage();
            }}
            type="checkbox"
          />
          Include deleted
        </label>
      </section>

      {loading ? (
        <section className="rounded-lg border border-border bg-surface px-5 py-10 text-sm text-muted">
          Loading users...
        </section>
      ) : null}

      {!loading && error ? (
        <section className="rounded-lg border border-error/30 bg-error/10 p-5">
          <p className="text-sm text-error">{error}</p>
          <button
            className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground"
            onClick={() => void loadUsers()}
            type="button"
          >
            Retry
          </button>
        </section>
      ) : null}

      {!loading && !error ? (
        <UserTable
          onManageAccess={setAccessUser}
          onView={setDetailUser}
          users={users}
        />
      ) : null}

      {!loading && !error ? (
        <section className="flex flex-col gap-3 rounded-lg border border-border bg-surface px-4 py-3 text-sm text-muted sm:flex-row sm:items-center sm:justify-between">
          <p>
            Page {currentPage} of {userPage.totalPages || 1} ·{" "}
            {userPage.totalElements} matching users
          </p>
          <div className="flex flex-wrap items-center gap-2">
            <label className="flex items-center gap-2">
              <span>Rows</span>
              <select
                aria-label="Rows per page"
                className="min-h-10 rounded-lg border border-border bg-background px-2 text-sm text-text"
                onChange={event => {
                  setSize(Number(event.target.value));
                  resetPage();
                }}
                value={size}
              >
                <option value={10}>10</option>
                <option value={20}>20</option>
                <option value={50}>50</option>
              </select>
            </label>
            <button
              className="min-h-10 rounded-lg border border-border px-3 font-medium text-text transition hover:border-primary hover:text-primary disabled:opacity-40"
              disabled={page === 0}
              onClick={() => setPage(current => Math.max(0, current - 1))}
              type="button"
            >
              Previous
            </button>
            <button
              className="min-h-10 rounded-lg border border-border px-3 font-medium text-text transition hover:border-primary hover:text-primary disabled:opacity-40"
              disabled={userPage.totalPages === 0 || page >= userPage.totalPages - 1}
              onClick={() => setPage(current => current + 1)}
              type="button"
            >
              Next
            </button>
          </div>
        </section>
      ) : null}

      {showCreate ? (
        <CreateUserDialog
          onClose={() => setShowCreate(false)}
          onCreated={created => {
            setUserPage(current => ({
              ...current,
              users: [created, ...current.users],
              totalElements: current.totalElements + 1
            }));
            setPage(0);
            setShowCreate(false);
          }}
        />
      ) : null}

      {detailUser ? (
        <UserDetailsDialog
          onClose={() => setDetailUser(null)}
          onDeleted={deleted => {
            replaceUser(deleted);
            void loadUsers();
          }}
          onEdit={user => setEditUser(user)}
          onManageAccess={user => {
            setDetailUser(null);
            setAccessUser(user);
          }}
          onUpdated={replaceUser}
          user={detailUser}
        />
      ) : null}

      {editUser ? (
        <EditUserDialog
          onClose={() => setEditUser(null)}
          onUpdated={replaceUser}
          user={editUser}
        />
      ) : null}

      {accessUser ? (
        <AccessDialog
          onClose={() => setAccessUser(null)}
          user={accessUser}
        />
      ) : null}
    </div>
  );
}
