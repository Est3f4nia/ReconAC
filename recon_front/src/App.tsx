import { Routes, Route } from "react-router-dom";

import AuthLayout from "@/components/layout/auth/AuthLayout";
import DashboardLayout from "@/components/layout/DashboardLayout";
import AuthPage from "@/pages/public/AuthPage";

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/" element={<AuthPage />} />
        <Route path="/register" element={<AuthPage />} />
      </Route>

      {/* rutas autenticadas */}
      <Route element={<DashboardLayout />}>
        {/* ... */}
      </Route>
    </Routes>
  );
}