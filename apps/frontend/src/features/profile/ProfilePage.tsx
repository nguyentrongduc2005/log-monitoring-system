import axios from "axios";
import { useEffect, useState } from "react";
import { PageHeader } from "@/shared/layouts/page-header-context";
import { useAuth } from "@/features/auth/auth-context";
import {
  changePassword,
  getProfile,
  updateProfile,
  type UserResponse
} from "@/features/profile/profile-api";

type ProfileForm = {
  email: string;
  displayName: string;
};

type PasswordForm = {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
};

function formatDate(value?: string) {
  if (!value) {
    return "Not available";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Not available";
  }

  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(date);
}

function getProfileErrorMessage(error: unknown) {
  if (axios.isAxiosError(error) && error.response?.status === 403) {
    return "You do not have permission to view this profile.";
  }

  return "Unable to load profile right now. Please try again.";
}

function getActionErrorMessage(fallback: string) {
  return fallback;
}

function getRoleDescription(role?: string) {
  if (role === "ADMIN") {
    return "Admins can manage users, roles, and system-wide operational settings.";
  }

  if (role === "ENGINEER") {
    return "Engineers can monitor the applications assigned to them and manage their own account.";
  }

  return "Role details are not available.";
}

function getStatusDescription(status?: string) {
  if (status === "ACTIVE") {
    return "Your account is active and can access the platform.";
  }

  if (status === "DISABLED") {
    return "Your account is disabled. Contact an administrator if this is unexpected.";
  }

  if (status === "LOCKED") {
    return "Your account is locked. Contact an administrator to unlock it.";
  }

  return "Status details are not available.";
}

export function Component() {
  const { updateSessionUser } = useAuth();
  const [profile, setProfile] = useState<UserResponse | null>(null);
  const [profileForm, setProfileForm] = useState<ProfileForm>({
    email: "",
    displayName: ""
  });
  const [passwordForm, setPasswordForm] = useState<PasswordForm>({
    oldPassword: "",
    newPassword: "",
    confirmPassword: ""
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [profileSuccess, setProfileSuccess] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null);
  const [savingProfile, setSavingProfile] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);

  async function loadProfile() {
    setLoading(true);
    setError(null);

    try {
      const nextProfile = await getProfile();
      setProfile(nextProfile);
      setProfileForm({
        email: nextProfile.email ?? "",
        displayName: nextProfile.displayName ?? ""
      });
    } catch (loadError) {
      setError(getProfileErrorMessage(loadError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    queueMicrotask(() => {
      void loadProfile();
    });
  }, []);

  async function handleProfileSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setProfileError(null);
    setProfileSuccess(null);

    const email = profileForm.email.trim();
    const displayName = profileForm.displayName.trim();

    if (!email || !displayName) {
      setProfileError("Email and display name are required.");
      return;
    }

    setSavingProfile(true);

    try {
      const updated = await updateProfile({ email, displayName });
      setProfile(updated);
      setProfileForm({
        email: updated.email ?? "",
        displayName: updated.displayName ?? ""
      });
      updateSessionUser(updated);
      setProfileSuccess("Profile updated successfully.");
    } catch {
      setProfileError(
        getActionErrorMessage("Unable to update profile right now. Please try again.")
      );
    } finally {
      setSavingProfile(false);
    }
  }

  async function handlePasswordSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(null);

    if (
      !passwordForm.oldPassword.trim() ||
      !passwordForm.newPassword.trim() ||
      !passwordForm.confirmPassword.trim()
    ) {
      setPasswordError("All password fields are required.");
      return;
    }

    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setPasswordError("Password confirmation does not match.");
      return;
    }

    setSavingPassword(true);

    try {
      await changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      });
      setPasswordForm({
        oldPassword: "",
        newPassword: "",
        confirmPassword: ""
      });
      setPasswordSuccess("Password changed successfully.");
    } catch {
      setPasswordError(
        getActionErrorMessage("Unable to change password right now. Please try again.")
      );
    } finally {
      setSavingPassword(false);
    }
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Profile" />

      {loading ? (
        <section className="rounded-2xl border border-border bg-surface px-5 py-10 text-sm text-muted">
          Loading profile...
        </section>
      ) : null}

      {!loading && error ? (
        <section className="rounded-2xl border border-border bg-surface px-5 py-6">
          <p className="text-sm text-error">{error}</p>
          <button
            className="mt-4 inline-flex min-h-10 items-center justify-center rounded-lg bg-primary px-4 text-sm font-medium text-black transition hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={() => void loadProfile()}
            type="button"
          >
            Retry
          </button>
        </section>
      ) : null}

      {!loading && !error && profile ? (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.4fr)_minmax(18rem,0.9fr)]">
          <div className="space-y-6">
            <section className="rounded-2xl border border-border bg-surface p-5">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-muted/70">
                    Account
                  </p>
                  <h2 className="mt-2 text-2xl font-semibold text-text">
                    {profile.displayName || "Unknown user"}
                  </h2>
                  <p className="mt-1 text-sm text-muted">
                    {profile.email || "No email available"}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2 text-xs font-medium">
                  <span className="rounded-full bg-primary/15 px-3 py-1 text-primary">
                    {profile.role || "Unknown role"}
                  </span>
                  <span className="rounded-full bg-success/15 px-3 py-1 text-success">
                    {profile.status || "Unknown status"}
                  </span>
                </div>
              </div>
              <dl className="mt-6 grid gap-4 sm:grid-cols-2">
                <div className="rounded-xl border border-border bg-surface-raised p-4">
                  <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
                    User ID
                  </dt>
                  <dd className="mt-2 break-all font-mono text-sm text-text">
                    {profile.id || "Not available"}
                  </dd>
                </div>
                <div className="rounded-xl border border-border bg-surface-raised p-4">
                  <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
                    Last login
                  </dt>
                  <dd className="mt-2 text-sm text-text">
                    {formatDate(profile.lastLoginAt)}
                  </dd>
                </div>
                <div className="rounded-xl border border-border bg-surface-raised p-4">
                  <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
                    Created
                  </dt>
                  <dd className="mt-2 text-sm text-text">
                    {formatDate(profile.createdAt)}
                  </dd>
                </div>
                <div className="rounded-xl border border-border bg-surface-raised p-4">
                  <dt className="text-xs uppercase tracking-[0.16em] text-muted/70">
                    Updated
                  </dt>
                  <dd className="mt-2 text-sm text-text">
                    {formatDate(profile.updatedAt)}
                  </dd>
                </div>
              </dl>
            </section>

            <section className="rounded-2xl border border-border bg-surface p-5">
              <div className="mb-5">
                <h3 className="text-lg font-semibold text-text">Edit profile</h3>
                <p className="mt-1 text-sm text-muted">
                  Update the account details used across the LogPulse workspace.
                </p>
              </div>
              <form className="space-y-4" onSubmit={handleProfileSubmit}>
                <label className="block space-y-2">
                  <span className="text-sm font-medium text-text">Display name</span>
                  <input
                    className="min-h-11 w-full rounded-xl border border-border bg-surface-raised px-3 text-sm text-text outline-none transition focus:border-primary"
                    name="displayName"
                    onChange={(event) =>
                      setProfileForm((current) => ({
                        ...current,
                        displayName: event.target.value
                      }))
                    }
                    value={profileForm.displayName}
                  />
                </label>
                <label className="block space-y-2">
                  <span className="text-sm font-medium text-text">Email</span>
                  <input
                    className="min-h-11 w-full rounded-xl border border-border bg-surface-raised px-3 text-sm text-text outline-none transition focus:border-primary"
                    name="email"
                    onChange={(event) =>
                      setProfileForm((current) => ({
                        ...current,
                        email: event.target.value
                      }))
                    }
                    type="email"
                    value={profileForm.email}
                  />
                </label>
                {profileError ? (
                  <p className="text-sm text-error">{profileError}</p>
                ) : null}
                {profileSuccess ? (
                  <p className="text-sm text-success">{profileSuccess}</p>
                ) : null}
                <button
                  className="inline-flex min-h-11 items-center justify-center rounded-xl bg-primary px-4 text-sm font-medium text-black transition hover:opacity-90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70 disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={savingProfile}
                  type="submit"
                >
                  {savingProfile ? "Saving..." : "Save profile"}
                </button>
              </form>
            </section>

            <section className="rounded-2xl border border-border bg-surface p-5">
              <div className="mb-5">
                <h3 className="text-lg font-semibold text-text">Change password</h3>
                <p className="mt-1 text-sm text-muted">
                  Rotate your password without leaving the current session.
                </p>
              </div>
              <form className="space-y-4" onSubmit={handlePasswordSubmit}>
                <label className="block space-y-2">
                  <span className="text-sm font-medium text-text">Current password</span>
                  <input
                    className="min-h-11 w-full rounded-xl border border-border bg-surface-raised px-3 text-sm text-text outline-none transition focus:border-primary"
                    name="oldPassword"
                    onChange={(event) =>
                      setPasswordForm((current) => ({
                        ...current,
                        oldPassword: event.target.value
                      }))
                    }
                    type="password"
                    value={passwordForm.oldPassword}
                  />
                </label>
                <label className="block space-y-2">
                  <span className="text-sm font-medium text-text">New password</span>
                  <input
                    className="min-h-11 w-full rounded-xl border border-border bg-surface-raised px-3 text-sm text-text outline-none transition focus:border-primary"
                    name="newPassword"
                    onChange={(event) =>
                      setPasswordForm((current) => ({
                        ...current,
                        newPassword: event.target.value
                      }))
                    }
                    type="password"
                    value={passwordForm.newPassword}
                  />
                </label>
                <label className="block space-y-2">
                  <span className="text-sm font-medium text-text">Confirm new password</span>
                  <input
                    className="min-h-11 w-full rounded-xl border border-border bg-surface-raised px-3 text-sm text-text outline-none transition focus:border-primary"
                    name="confirmPassword"
                    onChange={(event) =>
                      setPasswordForm((current) => ({
                        ...current,
                        confirmPassword: event.target.value
                      }))
                    }
                    type="password"
                    value={passwordForm.confirmPassword}
                  />
                </label>
                {passwordError ? (
                  <p className="text-sm text-error">{passwordError}</p>
                ) : null}
                {passwordSuccess ? (
                  <p className="text-sm text-success">{passwordSuccess}</p>
                ) : null}
                <button
                  className="inline-flex min-h-11 items-center justify-center rounded-xl border border-border bg-surface-raised px-4 text-sm font-medium text-text transition hover:border-primary hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70 disabled:cursor-not-allowed disabled:opacity-60"
                  disabled={savingPassword}
                  type="submit"
                >
                  {savingPassword ? "Updating..." : "Change password"}
                </button>
              </form>
            </section>
          </div>

          <aside className="space-y-6">
            <section className="rounded-2xl border border-border bg-surface p-5">
              <h3 className="text-lg font-semibold text-text">Access summary</h3>
              <div className="mt-4 space-y-4">
                <div className="rounded-xl border border-border bg-surface-raised p-4">
                  <p className="text-xs uppercase tracking-[0.16em] text-muted/70">
                    Role
                  </p>
                  <p className="mt-2 text-sm font-medium text-text">
                    {profile.role || "Unknown role"}
                  </p>
                  <p className="mt-2 text-sm text-muted">
                    {getRoleDescription(profile.role)}
                  </p>
                </div>
                <div className="rounded-xl border border-border bg-surface-raised p-4">
                  <p className="text-xs uppercase tracking-[0.16em] text-muted/70">
                    Status
                  </p>
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
        </div>
      ) : null}
    </div>
  );
}
