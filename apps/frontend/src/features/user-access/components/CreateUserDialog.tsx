import { useState, type FormEvent } from "react";
import {
  createUser,
  getUserAccessError
} from "@/features/user-access/user-access-api";
import type {
  User,
  UserRole
} from "@/features/user-access/user-access-types";
import DialogShell from "./DialogShell";

type CreateUserDialogProps = {
  onClose: () => void;
  onCreated: (user: User) => void;
};

const inputClasses =
  "mt-2 w-full rounded-xl border border-border bg-background px-3.5 py-3 text-sm text-text outline-none transition placeholder:text-muted/70 hover:border-muted/60 focus:border-primary focus:ring-2 focus:ring-primary/20";

export default function CreateUserDialog({
  onClose,
  onCreated
}: CreateUserDialogProps) {
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<UserRole>("ENGINEER");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);

    if (!email.trim() || !displayName.trim() || !password) {
      setError("Email, display name, and password are required.");
      return;
    }

    if (password.length < 6) {
      setError("Password must contain at least 6 characters.");
      return;
    }

    setSubmitting(true);
    try {
      const created = await createUser({
        email: email.trim(),
        displayName: displayName.trim(),
        password,
        role
      });
      onCreated(created);
    } catch (submitError) {
      setError(getUserAccessError(submitError, "Unable to create user."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <DialogShell
      description="Add a team member and choose their initial system role."
      eyebrow="User management"
      footer={
        <>
          <button
            className="min-h-11 rounded-xl border border-border px-5 text-sm font-medium text-text transition hover:bg-surface-raised focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70 disabled:opacity-50"
            disabled={submitting}
            onClick={onClose}
            type="button"
          >
            Cancel
          </button>
          <button
            className="min-h-11 rounded-xl bg-primary px-5 text-sm font-semibold text-black transition hover:bg-primary-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={submitting}
            form="create-user-form"
            type="submit"
          >
            {submitting ? "Creating user..." : "Create user"}
          </button>
        </>
      }
      onClose={onClose}
      title="Create user"
    >
      <form className="space-y-6" id="create-user-form" onSubmit={handleSubmit}>
        <section className="space-y-4">
          <div>
            <h3 className="text-sm font-semibold text-text">Account information</h3>
            <p className="mt-1 text-xs leading-5 text-muted">
              These details identify the user throughout the administration interface.
            </p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-sm font-medium text-text">
              Display name
              <input
                autoFocus
                className={inputClasses}
                maxLength={150}
                onChange={event => setDisplayName(event.target.value)}
                placeholder="Platform Engineer"
                value={displayName}
              />
            </label>

            <label className="block text-sm font-medium text-text">
              Email address
              <input
                aria-label="Email"
                className={inputClasses}
                maxLength={320}
                onChange={event => setEmail(event.target.value)}
                placeholder="engineer@example.com"
                type="email"
                value={email}
              />
            </label>
          </div>
        </section>

        <section className="space-y-4 border-t border-border pt-5">
          <div>
            <h3 className="text-sm font-semibold text-text">Security and role</h3>
            <p className="mt-1 text-xs leading-5 text-muted">
              The temporary password is only used for the user&apos;s first sign-in.
            </p>
          </div>
          <label className="block text-sm font-medium text-text">
            Temporary password
            <input
              aria-label="Temporary password"
              className={inputClasses}
              maxLength={100}
              minLength={6}
              onChange={event => setPassword(event.target.value)}
              placeholder="At least 6 characters"
              type="password"
              value={password}
            />
          </label>

          <fieldset>
            <legend className="text-sm font-medium text-text">System role</legend>
            <div className="mt-2 grid gap-3 sm:grid-cols-2">
              {(["ENGINEER", "ADMIN"] as const).map(option => (
                <label
                  className={`cursor-pointer rounded-xl border p-4 transition ${
                    role === option
                      ? "border-primary bg-primary/10 ring-1 ring-primary/30"
                      : "border-border bg-background hover:border-muted/70"
                  }`}
                  key={option}
                >
                  <input
                    className="sr-only"
                    name="role"
                    onChange={() => setRole(option)}
                    type="radio"
                    value={option}
                  />
                  <span className="block text-sm font-semibold text-text">
                    {option === "ADMIN" ? "Administrator" : "Engineer"}
                  </span>
                  <span className="mt-1 block text-xs leading-5 text-muted">
                    {option === "ADMIN"
                      ? "Full access to users, applications, and system settings."
                      : "Access only to applications explicitly assigned by an admin."}
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
        </section>

        {error ? (
          <p className="rounded-xl border border-error/30 bg-error/10 px-4 py-3 text-sm text-error">
            {error}
          </p>
        ) : null}
      </form>
    </DialogShell>
  );
}
