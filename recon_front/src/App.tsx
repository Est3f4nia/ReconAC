import { Routes, Route, Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";

import DashboardLayout from "@/components/layout/dashboard/DashboardLayout";
import AuthPage from "@/pages/public/AuthPage";
import DashboardPage from "@/pages/private/DashboardPage";
import AuditoriaDetailPage from "./pages/private/AuditoriaDetail";
import EscaneosListado from "./pages/private/EscaneosListado";

function ProtectedRoute() {
  const { isAuthenticated, loading } = useAuth();
  if (loading) return null;
  return isAuthenticated ? <Outlet /> : <Navigate to="/" replace />;
}

function AuthRedirect() {
  const { isAuthenticated } = useAuth();
  if (isAuthenticated) return <Navigate to="/dashboard" replace />;
  return <Outlet />;
}

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<AuthRedirect />}>
        <Route path="/" element={<AuthPage />} />
        <Route path="/register" element={<AuthPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route
              path="/auditorias/:auditoriaId"
              element={<AuditoriaDetailPage />}
            />
          <Route
              path="/auditorias/:auditoriaId/escaneos/:escaneoId"
              element={<AuditoriaDetailPage />}
            />
          <Route
              path="/escaneos"
              element={<EscaneosListado />}
            />
        </Route>
      </Route>
    </Routes>
  );
}
