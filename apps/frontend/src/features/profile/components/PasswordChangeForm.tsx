import type { FormEvent } from "react";
import {
  managementButtonClass,
  managementInputClass,
  managementPanelClass
} from "@/shared/components/management-ui";

export type PasswordForm = {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
};

type PasswordChangeFormProps = {
  error: string | null;
  form: PasswordForm;
  saving: boolean;
  success: string | null;
  onChange: (form: PasswordForm) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
};

export default function PasswordChangeForm({
  error,
  form,
  saving,
  success,
  onChange,
  onSubmit
}: PasswordChangeFormProps) {
  return (
    <section className={`${managementPanelClass} p-4`}>
      <div className="mb-4">
        <h3 className="text-base font-semibold text-text">Change password</h3>
        <p className="mt-1 text-sm text-muted">
          Rotate your password without leaving the current session.
        </p>
      </div>
      <form className="space-y-4" onSubmit={onSubmit}>
        <label className="block space-y-2">
          <span className="text-sm font-medium text-text">Current password</span>
          <input
            className={managementInputClass}
            name="oldPassword"
            onChange={event =>
              onChange({ ...form, oldPassword: event.target.value })
            }
            type="password"
            value={form.oldPassword}
          />
        </label>
        <label className="block space-y-2">
          <span className="text-sm font-medium text-text">New password</span>
          <input
            className={managementInputClass}
            name="newPassword"
            onChange={event =>
              onChange({ ...form, newPassword: event.target.value })
            }
            type="password"
            value={form.newPassword}
          />
        </label>
        <label className="block space-y-2">
          <span className="text-sm font-medium text-text">Confirm new password</span>
          <input
            className={managementInputClass}
            name="confirmPassword"
            onChange={event =>
              onChange({ ...form, confirmPassword: event.target.value })
            }
            type="password"
            value={form.confirmPassword}
          />
        </label>
        {error ? <p className="text-sm text-error">{error}</p> : null}
        {success ? <p className="text-sm text-success">{success}</p> : null}
        <button className={managementButtonClass} disabled={saving} type="submit">
          {saving ? "Updating..." : "Change password"}
        </button>
      </form>
    </section>
  );
}

