import { useEffect, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { fetchResumen, type EscaneoResumen } from "@/data/escaneos";
import { AuditCard } from "@/components/AuditoriaCard";
import "@/pages/global.css";
import "@/pages/private/styles/DashboardPage.css";

export default function DashboardPage() {
  const { user } = useAuth();
  const [auditorias, setAuditorias] = useState<EscaneoResumen[]>([]);
  const [cargando, setCargando] = useState(true);

  const nombre = user?.email.split("@")[0] ?? "Usuario";
  const nombreCapitalizado = nombre.charAt(0).toUpperCase() + nombre.slice(1);

  useEffect(() => {
    fetchResumen()
      .then(setAuditorias)
      .catch(() => {})
      .finally(() => setCargando(false));
  }, []);

  return (
    <div className="dashboard-page">
      <h1>Hola, {nombreCapitalizado}</h1>

      <section className="auditorias-section">
        <h2 className="auditorias-title">Auditorías</h2>

        {cargando ? (
          <p className="auditorias-empty">Cargando...</p>
        ) : auditorias.length === 0 ? (
          <p className="auditorias-empty">No hay auditorías</p>
        ) : (
          <div className="auditorias-grid">
            {auditorias.map((a) => (
              <AuditCard key={a.auditoriaId} audit={a} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
