import { useState } from "react";
import type { FormEvent } from "react";
import type { CreateApiKeyRequest } from "@/features/applications/application-types";
import DialogShell from "@/features/user-access/components/DialogShell";

type ApiKeyFormDialogProps = {
  saving: boolean;
  error?: string | null;
  onClose: () => void;
  onSubmit: (request: CreateApiKeyRequest) => void;
};

export default function ApiKeyFormDialog({
  saving,
  error,
  onClose,
  onSubmit
}: ApiKeyFormDialogProps) {
  const [name, setName] = useState("");
  const [expiresAt, setExpiresAt] = useState("");

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit({
      name: name.trim(),
      expiresAt: expiresAt ? new Date(expiresAt).toISOString() : undefined
    });
  }

  return (
    <DialogShell
      description="Create an API key for log ingestion. The raw secret is shown only once."
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
            disabled={saving || !name.trim()}
            form="api-key-form"
            type="submit"
          >
            {saving ? "Creating..." : "Create API key"}
          </button>
        </>
      }
      onClose={onClose}
      size="md"
      title="Create API key"
    >
      <form className="space-y-5" id="api-key-form" onSubmit={submit}>
        <label className="block">
          <span className="text-sm font-medium text-text">Key name</span>
          <input
            autoFocus
            className="mt-2 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => setName(event.target.value)}
            placeholder="Production ingest key"
            required
            value={name}
          />
        </label>
        <label className="block">
          <span className="text-sm font-medium text-text">Expires at</span>
          <input
            className="mt-2 w-full rounded-xl border border-border bg-background px-4 py-3 text-sm text-text outline-none focus:border-primary focus:ring-2 focus:ring-primary/20"
            onChange={event => setExpiresAt(event.target.value)}
            type="datetime-local"
            value={expiresAt}
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
