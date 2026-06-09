import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";

export default function ProtectedRoute() {
  const { session, isInitializing } = useAuth();
  const location = useLocation();

  if (isInitializing) {
    return (
      <main className="flex min-h-svh items-center justify-center bg-[#0b0d0f] text-sm text-[#c2c6d6]">
        Restoring secure session...
      </main>
    );
  }

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return <Outlet />;
}
