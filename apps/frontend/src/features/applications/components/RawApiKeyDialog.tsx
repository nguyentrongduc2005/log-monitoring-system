import type { ApiKeyCreation } from "@/features/applications/application-types";
import DialogShell from "@/features/user-access/components/DialogShell";

type RawApiKeyDialogProps = {
  apiKey: ApiKeyCreation;
  onClose: () => void;
};

export default function RawApiKeyDialog({
  apiKey,
  onClose
}: RawApiKeyDialogProps) {
  return (
    <DialogShell
      description="Copy this key now. For security, the raw secret will not be shown again."
      footer={
        <button
          className="min-h-11 rounded-lg bg-primary px-5 text-sm font-semibold text-primary-foreground transition hover:bg-primary-hover"
          onClick={onClose}
          type="button"
        >
          I copied this key
        </button>
      }
      onClose={onClose}
      size="lg"
      title="API key created"
    >
      <div className="space-y-4">
        <div className="rounded-lg border border-warning/30 bg-warning/10 p-4">
          <p className="text-sm font-medium text-warning">
            This is the only time the full API key is visible.
          </p>
        </div>
        <div>
          <p className="mb-2 text-sm font-medium text-text">Raw API key</p>
          <code className="block overflow-x-auto rounded-lg border border-border bg-background p-4 font-mono text-sm text-primary">
            {apiKey.rawApiKey}
          </code>
        </div>
        <div className="grid gap-3 sm:grid-cols-3">
          <div className="rounded-lg border border-border bg-background p-3">
            <p className="text-xs uppercase tracking-wide text-muted">Name</p>
            <p className="mt-1 text-sm font-medium text-text">{apiKey.name}</p>
          </div>
          <div className="rounded-lg border border-border bg-background p-3">
            <p className="text-xs uppercase tracking-wide text-muted">Prefix</p>
            <p className="mt-1 font-mono text-sm text-text">
              {apiKey.keyPrefix}
            </p>
          </div>
          <div className="rounded-lg border border-border bg-background p-3">
            <p className="text-xs uppercase tracking-wide text-muted">Expires</p>
            <p className="mt-1 text-sm text-text">
              {apiKey.expiresAt ? new Date(apiKey.expiresAt).toLocaleString() : "Never"}
            </p>
          </div>
        </div>
      </div>
    </DialogShell>
  );
}
