import { useEffect, useState } from "react";
import { useAuth } from "@/hooks/useAuth";
import { createAuditoria } from "@/data/auditorias";
import {
  fetchResumenAuditorias,
  type AuditoriaResumen,
} from "@/data/auditorias";
import { AuditCard } from "@/components/AuditoriaCard";
import "@/pages/global.css";
import "@/pages/private/styles/DashboardPage.css";

export default function DashboardPage() {
  const { user } = useAuth();

  const [auditorias, setAuditorias] = useState<AuditoriaResumen[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState("");
  const [mostrarForm, setMostrarForm] = useState(false);
  const [nombre, setNombre] = useState("");
  const [objetivo, setObjetivo] = useState("");
  const [enviando, setEnviando] = useState(false);

  const emailNombre = user?.email?.split("@")[0] ?? "Usuario";

  const nombreCapitalizado =
    emailNombre.charAt(0).toUpperCase() + emailNombre.slice(1);

  useEffect(() => {
    cargarAuditorias();
  }, []);

  async function cargarAuditorias() {
  try {
    setCargando(true);
    setError("");

    const data = await fetchResumenAuditorias();
    setAuditorias(data);
  } catch (err) {
    setError(
      err instanceof Error
        ? err.message
        : "Error al cargar las auditorías"
    );
  } finally {
    setCargando(false);
  }
}

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    setError("");
    setEnviando(true);

    try {
      await createAuditoria(nombre, objetivo);

      setNombre("");
      setObjetivo("");
      setMostrarForm(false);

      // Recarga el resumen para obtener la auditoría
      // exactamente como la devuelve el backend.
      await cargarAuditorias();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Error al crear la auditoría"
      );
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
            className="button auditorias-nueva-btn"
            onClick={() => setMostrarForm(true)}
          >
            Nueva Auditoría
          </button>
        </div>

        {error && <p className="modal-error">{error}</p>}

        {cargando ? (
          <p className="auditorias-empty">Cargando...</p>
        ) : auditorias.length === 0 ? (
          <p className="auditorias-empty">No hay auditorías</p>
        ) : (
          <div className="auditorias-grid">
            {auditorias.map((auditoria) => (
              <AuditCard
                key={auditoria.auditoriaId}
                audit={auditoria}
              />
            ))}
          </div>
        )}
      </section>

      {mostrarForm && (
        <div
          className="modal-overlay"
          onClick={() => setMostrarForm(false)}
        >
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="modal-title">Nueva Auditoría</h2>

            <form className="modal-form" onSubmit={handleSubmit}>
              <div className="field">
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

              <div className="field">
                <label htmlFor="objetivo">Descripción</label>
                <textarea
                  id="objetivo"
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
