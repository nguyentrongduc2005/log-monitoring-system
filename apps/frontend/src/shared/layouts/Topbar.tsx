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
    <header className="flex min-h-14 items-center gap-3 border-b border-[#23252a] bg-[#0f1011]/95 px-3 backdrop-blur sm:px-4 lg:px-6">
      <button
        aria-controls="application-sidebar"
        aria-expanded={sidebarOpen}
        aria-label={sidebarOpen ? "Hide navigation" : "Show navigation"}
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md border border-transparent text-[#8a8f98] transition-colors hover:border-[#34343a] hover:bg-[#18191a] hover:text-[#f7f8f8] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#5e6ad2]/70"
        onClick={onToggleSidebar}
        ref={toggleRef}
        type="button"
      >
        <AppIcon name="menu" />
      </button>

      <div className="flex min-w-0 flex-1 flex-wrap items-center gap-3">
        <h1
          className="min-w-0 flex-1 truncate text-sm font-semibold tracking-[-0.1px] text-[#f7f8f8] sm:text-base"
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
