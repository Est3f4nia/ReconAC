import { useAuth } from "@/hooks/useAuth";
import "@/pages/global.css";

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="dashboard-page">
      <h1>Hola, {user?.email.split("@")[0] ?? "Usuario"}</h1>

      <section className="session-info">
        <h2>Información de sesión</h2>
        <dl>
          <dt>Email</dt>
          <dd>{user?.email}</dd>

          <dt>Rol</dt>
          <dd>{user?.roles.join(", ") ?? "—"}</dd>
        </dl>
      </section>
    </div>
  );
}
