import { Routes, Route, Navigate, Outlet } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";

import DashboardLayout from "@/components/layout/dashboard/DashboardLayout";
import AuthLayout from "@/components/layout/auth/AuthLayout";
import AuthPage from "@/pages/public/AuthPage";
import DashboardPage from "@/pages/private/DashboardPage";
import AuditoriaDetailPage from "@/pages/private/AuditoriaDetailPage";
import EscaneosListadoPage from "@/pages/private/EscaneosListadoPage";
import ActivosListadoPage from "@/pages/private/ActivosListadoPage";

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
      {/* Rutas públicas (auth) */}
      <Route element={<AuthRedirect />}>
        <Route element={<AuthLayout />}>
          <Route path="/" element={<AuthPage />} />
          <Route path="/register" element={<AuthPage />} />
        </Route>
      </Route>

      {/* Rutas privadas */}
      <Route element={<ProtectedRoute />}>
        <Route element={<DashboardLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/escaneos" element={<EscaneosListadoPage />} />
          <Route path="/activos" element={<ActivosListadoPage />} />

          <Route path="/auditorias/:auditoriaId" element={<AuditoriaDetailPage />} />
          <Route path="/auditorias/:auditoriaId/escaneos/:escaneoId" element={<AuditoriaDetailPage />} />
        </Route>
      </Route>
    </Routes>
  );
}