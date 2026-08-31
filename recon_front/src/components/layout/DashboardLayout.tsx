import { Outlet } from "react-router-dom";
import { Navbar } from "@/components/Navbar";
// import "./DashboardLayout.css";

export default function DashboardLayout() {
  return (
    <div className="dashboard-layout">
        <Navbar
            username="User"
            items={[
                { label: "Escaneos", path: "/escaneos" },  // ver por rdd
                { label: "Activos", path: "/activos" },
            ]}
        />

        <main className="dashboard-content">
            <Outlet />
        </main>
    </div>
  );
}