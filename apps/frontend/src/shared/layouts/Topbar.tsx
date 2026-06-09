import type { RefObject } from "react";
import type { components } from "@/api/generated/api-types";
import { AppIcon } from "@/shared/components/AppIcon";
import { usePageHeaderSlots } from "@/shared/layouts/page-header-context";
import UserMenu from "@/shared/layouts/UserMenu";

type UserResponse = components["schemas"]["UserResponse"];

type TopbarProps = {
  sidebarOpen: boolean;
  onToggleSidebar: () => void;
  user: UserResponse;
  onLogout: () => void;
  toggleRef: RefObject<HTMLButtonElement | null>;
};

export default function Topbar({
  sidebarOpen,
  onToggleSidebar,
  user,
  onLogout,
  toggleRef
}: TopbarProps) {
  const { setActionsTarget, setTitleTarget } = usePageHeaderSlots();

  return (
    <header className="flex min-h-16 items-center gap-3 border-b border-border bg-header px-3 sm:px-4 lg:px-6">
      <button
        aria-controls="application-sidebar"
        aria-expanded={sidebarOpen}
        aria-label={sidebarOpen ? "Hide navigation" : "Show navigation"}
        className="flex h-10 w-10 shrink-0 items-center justify-center rounded-md text-muted transition-colors hover:bg-surface-raised hover:text-text focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/70"
        onClick={onToggleSidebar}
        ref={toggleRef}
        type="button"
      >
        <AppIcon name="menu" />
      </button>

      <div className="flex min-w-0 flex-1 flex-wrap items-center gap-3">
        <h1
          className="min-w-0 flex-1 truncate text-base font-semibold text-text sm:text-lg"
          ref={setTitleTarget}
        />
        <div
          className="flex shrink-0 flex-wrap items-center gap-2"
          ref={setActionsTarget}
        />
      </div>

      <UserMenu
        displayName={user.displayName}
        email={user.email}
        onLogout={onLogout}
        role={user.role}
      />
    </header>
  );
}
