import { useState } from "react";
import type { FormEvent } from "react";
import type {
  Application,
  ApplicationRequest
} from "@/features/applications/application-types";
import DialogShell from "@/features/user-access/components/DialogShell";

type ApplicationFormDialogProps = {
  application?: Application;
  saving: boolean;
  error?: string | null;
  onClose: () => void;
  onSubmit: (request: ApplicationRequest) => void;
};

export default function ApplicationFormDialog({
  application,
  saving,
  error,
  onClose,
  onSubmit
}: ApplicationFormDialogProps) {
  const [name, setName] = useState(application?.name || "");
  const [displayName, setDisplayName] = useState(
    application?.displayName || ""
  );
  const [description, setDescription] = useState(
    application?.description || ""
  );

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit({
      name: name.trim(),
      displayName: displayName.trim(),
      description: description.trim() || undefined
    });
  }

  return (
    <DialogShell
      description="Define the service identity that can send logs and receive access policies."
      footer={
        <>
          <button
            className="min-h-11 rounded-lg border border-border px-5 text-sm font-medium text-text transition hover:bg-surface-raised"
            disabled={saving}
            onClick={onClose}
            type="button"
          >
            Cancel
          </button>
          <button
            className="min-h-11 rounded-lg bg-primary px-5 text-sm font-semibold text-primary-foreground transition hover:bg-primary-hover disabled:opacity-50"
            disabled={saving || !name.trim() || !displayName.trim()}
            form="application-form"
            type="submit"
          >
            {saving ? "Saving..." : application ? "Save changes" : "Create app"}
          </button>
        </>
      }
      onClose={onClose}
      size="lg"
      title={application ? "Edit application" : "Create application"}
    >
      <form className="space-y-5" id="application-form" onSubmit={submit}>
        <label className="block">
          <span className="text-sm font-medium text-text">Application name</span>
          <input
            autoFocus
            className="mt-2 w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => setName(event.target.value)}
            placeholder="billing-service"
            required
            value={name}
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-text">Display name</span>
          <input
            className="mt-2 w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => setDisplayName(event.target.value)}
            placeholder="Billing Service"
            required
            value={displayName}
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-text">Description</span>
          <textarea
            className="mt-2 min-h-28 w-full rounded-lg border border-border bg-background px-4 py-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => setDescription(event.target.value)}
            placeholder="What does this application do?"
            value={description}
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
