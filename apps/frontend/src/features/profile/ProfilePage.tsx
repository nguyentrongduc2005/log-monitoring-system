import axios from "axios";
import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { PageHeader } from "@/shared/layouts/page-header-context";
import { useAuth } from "@/features/auth/auth-context";
import {
  changePassword,
  getProfile,
  updateProfile,
  type UserResponse
} from "@/features/profile/profile-api";
import PasswordChangeForm from "./components/PasswordChangeForm";
import type { PasswordForm } from "./components/PasswordChangeForm";
import ProfileAccessSummary from "./components/ProfileAccessSummary";
import ProfileEditForm from "./components/ProfileEditForm";
import type { ProfileForm } from "./components/ProfileEditForm";
import ProfileIdentityCard from "./components/ProfileIdentityCard";
import {
  managementButtonClass,
  managementPanelClass
} from "@/shared/components/management-ui";

function getProfileErrorMessage(error: unknown) {
  if (axios.isAxiosError(error) && error.response?.status === 403) {
    return "You do not have permission to view this profile.";
  }

  return "Unable to load profile right now. Please try again.";
}

function getActionErrorMessage(fallback: string) {
  return fallback;
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

  async function handleProfileSubmit(event: FormEvent<HTMLFormElement>) {
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

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
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
    <div className="space-y-5">
      <PageHeader title="Profile" />

      {loading ? (
        <section className={`${managementPanelClass} px-4 py-8 text-sm text-muted`}>
          Loading profile...
        </section>
      ) : null}

      {!loading && error ? (
        <section className={`${managementPanelClass} px-4 py-5`}>
          <p className="text-sm text-error">{error}</p>
          <button
            className={`mt-4 ${managementButtonClass}`}
            onClick={() => void loadProfile()}
            type="button"
          >
            Retry
          </button>
        </section>
      ) : null}

      {!loading && !error && profile ? (
        <div className="grid gap-5 xl:grid-cols-[minmax(0,1.4fr)_minmax(18rem,0.9fr)]">
          <div className="space-y-5">
            <ProfileIdentityCard profile={profile} />
            <ProfileEditForm
              error={profileError}
              form={profileForm}
              onChange={setProfileForm}
              onSubmit={event => void handleProfileSubmit(event)}
              saving={savingProfile}
              success={profileSuccess}
            />
            <PasswordChangeForm
              error={passwordError}
              form={passwordForm}
              onChange={setPasswordForm}
              onSubmit={event => void handlePasswordSubmit(event)}
              saving={savingPassword}
              success={passwordSuccess}
            />
          </div>

          <div className="xl:sticky xl:top-6 xl:self-start">
            <ProfileAccessSummary profile={profile} />
          </div>
        </div>
      ) : null}
    </div>
  );
}
