import {
  useEffect,
  useId,
  useRef,
  type PropsWithChildren,
  type ReactNode
} from "react";

type DialogShellProps = PropsWithChildren<{
  title: string;
  description?: string;
  footer?: ReactNode;
  onClose: () => void;
  size?: "md" | "lg" | "xl";
  eyebrow?: string;
}>;

const sizeClasses = {
  md: "max-w-xl",
  lg: "max-w-3xl",
  xl: "max-w-5xl"
};

export default function DialogShell({
  title,
  description,
  footer,
  onClose,
  size = "md",
  eyebrow,
  children
}: DialogShellProps) {
  const titleId = useId();
  const descriptionId = useId();
  const dialogRef = useRef<HTMLElement>(null);

  useEffect(() => {
    const previousActiveElement = document.activeElement as HTMLElement | null;
    const dialog = dialogRef.current;
    const focusableSelector =
      'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href], [tabindex]:not([tabindex="-1"])';

    dialog?.querySelector<HTMLElement>("[autofocus]")?.focus();
    if (!dialog?.contains(document.activeElement)) {
      dialog?.querySelector<HTMLElement>(focusableSelector)?.focus();
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
        return;
      }

      if (event.key !== "Tab" || !dialog) {
        return;
      }

      const focusableElements =
        Array.from(dialog.querySelectorAll<HTMLElement>(focusableSelector));
      const firstElement = focusableElements[0];
      const lastElement = focusableElements.at(-1);

      if (focusableElements.length === 0) {
        event.preventDefault();
        return;
      }

      if (event.shiftKey && document.activeElement === firstElement) {
        event.preventDefault();
        lastElement?.focus();
      } else if (!event.shiftKey && document.activeElement === lastElement) {
        event.preventDefault();
        firstElement?.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      document.body.style.overflow = "";
      previousActiveElement?.focus();
    };
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center p-0 sm:items-center sm:p-6">
      <button
        aria-label="Close dialog"
        className="absolute inset-0 cursor-default bg-black/75 backdrop-blur-sm"
        onClick={onClose}
        type="button"
      />
      <section
        aria-describedby={description ? descriptionId : undefined}
        aria-labelledby={titleId}
        aria-modal="true"
        className={`relative flex max-h-[94svh] w-full ${sizeClasses[size]} flex-col overflow-hidden rounded-t-3xl border border-border bg-surface shadow-[0_24px_80px_rgba(0,0,0,0.55)] sm:max-h-[88svh] sm:rounded-2xl`}
        ref={dialogRef}
        role="dialog"
      >
        <div className="mx-auto mt-2 h-1 w-10 rounded-full bg-border sm:hidden" />
        <header className="flex items-start justify-between gap-5 border-b border-border bg-surface-raised/35 px-5 py-5 sm:px-6">
          <div className="min-w-0">
            {eyebrow ? (
              <p className="mb-1 text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                {eyebrow}
              </p>
            ) : null}
            <h2 className="truncate text-xl font-semibold tracking-tight text-text" id={titleId}>
              {title}
            </h2>
            {description ? (
              <p className="mt-1.5 text-sm leading-5 text-muted" id={descriptionId}>
                {description}
              </p>
            ) : null}
          </div>
          <button
            aria-label="Close"
            className="grid size-9 shrink-0 place-items-center rounded-lg border border-transparent text-lg text-muted transition hover:border-border hover:bg-surface-raised hover:text-text focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={onClose}
            type="button"
          >
            <span aria-hidden="true">X</span>
          </button>
        </header>
        <div className="shell-scrollbar overflow-y-auto px-5 py-5 sm:px-6 sm:py-6">
          {children}
        </div>
        {footer ? (
          <footer className="flex flex-col-reverse gap-3 border-t border-border bg-surface-raised/35 px-5 py-4 sm:flex-row sm:justify-end sm:px-6">
            {footer}
          </footer>
        ) : null}
      </section>
    </div>
  );
}
