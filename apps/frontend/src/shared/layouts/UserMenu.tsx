import { useEffect, useId, useRef, useState } from "react";
import { AppIcon } from "@/shared/components/AppIcon";

type UserMenuProps = {
  displayName?: string;
  email?: string;
  role?: string;
  onLogout: () => void;
};

function getIdentity(displayName?: string, email?: string) {
  return displayName?.trim() || email?.trim() || "Account";
}

function getInitials(identity: string) {
  const words = identity
    .replace(/@.*$/, "")
    .split(/[\s._-]+/)
    .filter(Boolean);

  if (words.length === 0) {
    return "A";
  }

  return words
    .slice(0, 2)
    .map((word) => word[0])
    .join("")
    .toUpperCase();
}

function getRoleLabel(role?: string) {
  if (role === "ADMIN") {
    return "Admin";
  }

  if (role === "ENGINEER") {
    return "Engineer";
  }

  return "User";
}

export default function UserMenu({
  displayName,
  email,
  role,
  onLogout
}: UserMenuProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const menuId = useId();
  const identity = getIdentity(displayName, email);
  const roleLabel = getRoleLabel(role);

  useEffect(() => {
    if (!open) {
      return;
    }

    function handlePointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        setOpen(false);
        triggerRef.current?.focus();
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);

    return () => {
      document.removeEventListener("pointerdown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [open]);

  function handleLogout() {
    setOpen(false);
    onLogout();
  }

  return (
    <div className="relative" ref={rootRef}>
      <button
        aria-controls={open ? menuId : undefined}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label="Open account menu"
        className="flex min-h-10 items-center gap-2 rounded-md px-1.5 py-1 text-left transition-colors hover:bg-surface-raised focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70 sm:px-2"
        onClick={() => setOpen((value) => !value)}
        ref={triggerRef}
        type="button"
      >
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/15 text-xs font-semibold text-primary">
          {getInitials(identity)}
        </span>
        <span className="hidden min-w-0 max-w-40 sm:block">
          <span
            className="block truncate text-sm font-medium text-text"
            title={identity}
          >
            {identity}
          </span>
          <span className="block text-[11px] text-muted">{roleLabel}</span>
        </span>
        <AppIcon
          className="hidden shrink-0 text-muted sm:block"
          name="chevron-down"
          size={16}
        />
      </button>

      {open ? (
        <div
          className="absolute right-0 top-[calc(100%+8px)] z-50 w-60 rounded-md border border-border bg-surface p-1 shadow-2xl"
          id={menuId}
          role="menu"
        >
          <div className="border-b border-border px-3 py-2 sm:hidden">
            <p className="truncate text-sm font-medium text-text" title={identity}>
              {identity}
            </p>
            <p className="text-xs text-muted">{roleLabel}</p>
          </div>
          <button
            className="flex min-h-10 w-full items-center gap-2 rounded px-3 py-2 text-sm text-muted hover:bg-surface-raised hover:text-text focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={() => setOpen(false)}
            role="menuitem"
            type="button"
          >
            <AppIcon name="user" size={18} />
            Hồ sơ cá nhân
          </button>
          <div className="my-1 h-px bg-border" role="separator" />
          <button
            className="flex min-h-10 w-full items-center gap-2 rounded px-3 py-2 text-sm text-muted hover:bg-surface-raised hover:text-text focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
            onClick={handleLogout}
            role="menuitem"
            type="button"
          >
            <AppIcon name="logout" size={18} />
            Log out
          </button>
        </div>
      ) : null}
    </div>
  );
}
