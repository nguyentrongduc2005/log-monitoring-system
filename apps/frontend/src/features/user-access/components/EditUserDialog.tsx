import { useState } from "react";
import type { FormEvent } from "react";
import {
  getUserAccessError,
  updateUser
} from "@/features/user-access/user-access-api";
import type { User } from "@/features/user-access/user-access-types";
import DialogShell from "./DialogShell";

type EditUserDialogProps = {
  user: User;
  onClose: () => void;
  onUpdated: (user: User) => void;
};

export default function EditUserDialog({
  user,
  onClose,
  onUpdated
}: EditUserDialogProps) {
  const [displayName, setDisplayName] = useState(user.displayName || "");
  const [email, setEmail] = useState(user.email || "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!user.id) {
      return;
    }

    setSaving(true);
    setError(null);
    try {
      const updated = await updateUser(user.id, {
        displayName: displayName.trim(),
        email: email.trim()
      });
      onUpdated(updated);
      onClose();
    } catch (saveError) {
      setError(getUserAccessError(saveError, "Unable to update user."));
    } finally {
      setSaving(false);
    }
  }

  return (
    <DialogShell
      description={user.email}
      footer={
        <>
          <button
            className="min-h-11 rounded-xl border border-border px-5 text-sm font-medium text-text transition hover:bg-surface-raised"
            disabled={saving}
            onClick={onClose}
            type="button"
          >
            Cancel
          </button>
          <button
            className="min-h-11 rounded-xl bg-primary px-5 text-sm font-semibold text-black transition hover:bg-primary-hover disabled:opacity-50"
            disabled={saving || !email.trim()}
            form="edit-user-form"
            type="submit"
          >
            {saving ? "Saving..." : "Save changes"}
          </button>
        </>
      }
      onClose={onClose}
      size="lg"
      title="Edit user"
    >
      <form className="space-y-5" id="edit-user-form" onSubmit={submit}>
        <label className="block">
          <span className="text-sm font-medium text-text">Display name</span>
          <input
            autoFocus
            className="mt-2 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => setDisplayName(event.target.value)}
            value={displayName}
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-text">Email</span>
          <input
            className="mt-2 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => setEmail(event.target.value)}
            required
            type="email"
            value={email}
          />
        </label>

        {error ? (
          <p className="rounded-lg border border-error/30 bg-error/10 px-3 py-2 text-sm text-error">
            {error}
          </p>
        ) : null}
      </form>
    </DialogShell>
  );
}
