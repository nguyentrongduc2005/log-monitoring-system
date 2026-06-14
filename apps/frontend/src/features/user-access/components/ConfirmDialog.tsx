import DialogShell from "./DialogShell";

type ConfirmDialogProps = {
  title: string;
  description: string;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: "danger" | "warning";
  loading?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
};

export default function ConfirmDialog({
  title,
  description,
  confirmLabel,
  cancelLabel = "Cancel",
  tone = "warning",
  loading = false,
  onCancel,
  onConfirm
}: ConfirmDialogProps) {
  const confirmClasses =
    tone === "danger"
      ? "bg-error text-white hover:bg-error/90"
      : "bg-warning text-black hover:bg-warning/90";

  return (
    <DialogShell
      footer={
        <>
          <button
            className="min-h-11 rounded-xl border border-border px-5 text-sm font-medium text-text transition hover:bg-surface-raised"
            disabled={loading}
            onClick={onCancel}
            type="button"
          >
            {cancelLabel}
          </button>
          <button
            className={`min-h-11 rounded-xl px-5 text-sm font-semibold transition disabled:opacity-50 ${confirmClasses}`}
            disabled={loading}
            onClick={onConfirm}
            type="button"
          >
            {loading ? "Working..." : confirmLabel}
          </button>
        </>
      }
      onClose={onCancel}
      size="md"
      title={title}
    >
      <div className="rounded-2xl border border-border bg-background p-5">
        <p className="text-sm leading-6 text-muted">{description}</p>
      </div>
    </DialogShell>
  );
}
