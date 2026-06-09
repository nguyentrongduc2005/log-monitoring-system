import { useEffect, useRef, useState } from "react";
import { Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";
import MobileSidebarDrawer from "@/shared/layouts/MobileSidebarDrawer";
import { PageHeaderProvider } from "@/shared/layouts/page-header-context";
import Sidebar from "@/shared/layouts/Sidebar";
import Topbar from "@/shared/layouts/Topbar";

const desktopQuery = "(min-width: 768px)";

function AppLayoutContent() {
  const { session, logout } = useAuth();
  const initialDesktop = window.matchMedia(desktopQuery).matches;
  const [isDesktop, setIsDesktop] = useState(initialDesktop);
  const [desktopSidebarOpen, setDesktopSidebarOpen] = useState(initialDesktop);
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);
  const toggleRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const mediaQuery = window.matchMedia(desktopQuery);

    function handleViewportChange(event: MediaQueryListEvent) {
      setIsDesktop(event.matches);
      setMobileDrawerOpen(false);
      setDesktopSidebarOpen(event.matches);
    }

    mediaQuery.addEventListener("change", handleViewportChange);
    return () => mediaQuery.removeEventListener("change", handleViewportChange);
  }, []);

  if (!session) {
    return null;
  }

  const navigationOpen = isDesktop ? desktopSidebarOpen : mobileDrawerOpen;

  function toggleNavigation() {
    if (isDesktop) {
      setDesktopSidebarOpen((open) => !open);
    } else {
      setMobileDrawerOpen((open) => !open);
    }
  }

  return (
    <div className="h-svh overflow-hidden bg-background text-text">
      {isDesktop && desktopSidebarOpen ? (
        <div className="fixed inset-y-0 left-0 z-40 w-60">
          <Sidebar id="application-sidebar" role={session.user.role} />
        </div>
      ) : null}

      <MobileSidebarDrawer
        onClose={() => setMobileDrawerOpen(false)}
        open={!isDesktop && mobileDrawerOpen}
        returnFocusRef={toggleRef}
      >
        <Sidebar
          onNavigate={() => setMobileDrawerOpen(false)}
          onRequestClose={() => setMobileDrawerOpen(false)}
          role={session.user.role}
          showCloseButton
        />
      </MobileSidebarDrawer>

      <div
        className={`flex h-full min-w-0 flex-col transition-[padding-left] duration-200 ${
          isDesktop && desktopSidebarOpen ? "md:pl-60" : ""
        }`}
        data-testid="app-content-column"
      >
        <Topbar
          onLogout={logout}
          onToggleSidebar={toggleNavigation}
          sidebarOpen={navigationOpen}
          toggleRef={toggleRef}
          user={session.user}
        />
        <main className="shell-scrollbar min-w-0 flex-1 overflow-y-auto bg-background p-3 sm:p-4 lg:p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export default function AppLayout() {
  return (
    <PageHeaderProvider>
      <AppLayoutContent />
    </PageHeaderProvider>
  );
}
