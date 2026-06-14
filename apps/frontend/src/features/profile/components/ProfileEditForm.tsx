import type { FormEvent } from "react";
import {
  managementInputClass,
  managementPanelClass,
  managementPrimaryButtonClass
} from "@/shared/components/management-ui";

export type ProfileForm = {
  email: string;
  displayName: string;
};

type ProfileEditFormProps = {
  error: string | null;
  form: ProfileForm;
  saving: boolean;
  success: string | null;
  onChange: (form: ProfileForm) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export default function ProfileEditForm({
  error,
  form,
  saving,
  success,
  onChange,
  onSubmit
}: ProfileEditFormProps) {
  return (
    <section className={`${managementPanelClass} p-4`}>
      <div className="mb-4">
        <h3 className="text-base font-semibold text-text">Edit profile</h3>
        <p className="mt-1 text-sm text-muted">
          Update the account details used across the LogPulse workspace.
        </p>
      </div>
      <form className="space-y-4" onSubmit={onSubmit}>
        <label className="block space-y-2">
          <span className="text-sm font-medium text-text">Display name</span>
          <input
            className={managementInputClass}
            name="displayName"
            onChange={event =>
              onChange({ ...form, displayName: event.target.value })
            }
            value={form.displayName}
          />
        </label>
        <label className="block space-y-2">
          <span className="text-sm font-medium text-text">Email</span>
          <input
            className={managementInputClass}
            name="email"
            onChange={event => onChange({ ...form, email: event.target.value })}
            type="email"
            value={form.email}
          />
        </label>
        {error ? <p className="text-sm text-error">{error}</p> : null}
        {success ? <p className="text-sm text-success">{success}</p> : null}
        <button
          className={managementPrimaryButtonClass}
          disabled={saving}
          type="submit"
        >
          {saving ? "Saving..." : "Save profile"}
        </button>
      </form>
    </section>
  );
}

