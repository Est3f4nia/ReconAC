import { Outlet } from "react-router-dom";
import { Navbar } from "@/components/Navbar";
import "./DashboardStyle.css"

export default function DashboardLayout() {
  return (
    <div className="dashboard-layout">
        <Navbar
            items={[
                { label: "Escaneos", path: "/escaneos" },
                { label: "Activos", path: "/activos" },
            ]}
        />

        <main className="dashboard-content">
            <Outlet />
        </main>
    </div>
  );
}