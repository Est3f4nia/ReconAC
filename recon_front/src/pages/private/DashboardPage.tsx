import { useEffect, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { fetchResumen, createAuditoria, type EscaneoResumen } from "@/data/escaneos";
import { AuditCard } from "@/components/AuditoriaCard";
import "@/pages/global.css";
import "@/pages/private/styles/DashboardPage.css";

export default function DashboardPage() {
  const { user } = useAuth();
  const [auditorias, setAuditorias] = useState<EscaneoResumen[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [mostrarForm, setMostrarForm] = useState(false);
  const [nombre, setNombre] = useState("");
  const [objetivo, setObjetivo] = useState("");
  const [enviando, setEnviando] = useState(false);

  const nombreCapitalizado = (user?.email.split("@")[0] ?? "Usuario").charAt(0).toUpperCase() + (user?.email.split("@")[0] ?? "Usuario").slice(1);

  useEffect(() => {
    fetchResumen()
      .then(setAuditorias)
      .catch((err) => setError(err instanceof Error ? err.message : "Error al cargar las auditorías"))
      .finally(() => setCargando(false));
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setEnviando(true);
    try {
      const response = await createAuditoria(nombre, objetivo);
      const nueva: EscaneoResumen = {
        escaneoId: null,
        auditoriaId: response.id,
        auditoriaNombre: response.nombre,
        activos: 0,
        puertos: 0,
        ultimoEscaneo: null,
        status: "QUEUED",
        cve: 0,
        cveCriticos: 0,
      };
      setAuditorias((prev) => [nueva, ...prev]);
      setNombre("");
      setObjetivo("");
      setMostrarForm(false);
      const res = await fetchResumen();
      setAuditorias(res);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Error al crear la auditoría");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="dashboard-page">
      <h1>Hola, {nombreCapitalizado}</h1>

      <section className="auditorias-section">
        <div className="auditorias-header">
          <h2 className="auditorias-title">Auditorías</h2>
          <button
            className="auditorias-nueva-btn"
            onClick={() => setMostrarForm(true)}
          >
            Nueva Auditoría
          </button>
        </div>

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

      {mostrarForm && (
        <div className="modal-overlay" onClick={() => setMostrarForm(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2 className="modal-title">Nueva Auditoría</h2>

            {error && <p className="modal-error">{error}</p>}

            <form className="modal-form" onSubmit={handleSubmit}>
              <div className="modal-field">
                <label htmlFor="nombre">Nombre</label>
                <input
                  id="nombre"
                  type="text"
                  required
                  value={nombre}
                  onChange={(e) => setNombre(e.target.value)}
                  disabled={enviando}
                />
              </div>

              <div className="modal-field">
                <label htmlFor="objetivo">Objetivo</label>
                <textarea
                  id="objetivo"
                  required
                  value={objetivo}
                  onChange={(e) => setObjetivo(e.target.value)}
                  disabled={enviando}
                />
              </div>

              <div className="modal-actions">
                <button
                  type="submit"
                  className="modal-submit"
                  disabled={enviando}
                >
                  {enviando ? "Creando..." : "Crear"}
                </button>
                <button
                  type="button"
                  className="modal-cancel"
                  onClick={() => setMostrarForm(false)}
                  disabled={enviando}
                >
                  Cancelar
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
