import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/features/auth/auth-context";

export default function AdminRoute() {
  const { session } = useAuth();

  if (session?.user.role !== "ADMIN") {
    return <Navigate replace to="/" />;
  }

  return <Outlet />;
}
