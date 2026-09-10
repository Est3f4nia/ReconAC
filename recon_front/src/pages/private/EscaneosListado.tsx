import type {
  EscaneoListado as EscaneoListadoType,
  EscaneoResultResponse,
  AuditoriaResponse,
} from "@/data/types";
import "@/pages/private/styles/EscaneosListado.css";

interface Props {
  escaneos: EscaneoListadoType[];
  page: number;
  totalPages: number;
  onAnterior: () => void;
  onSiguiente: () => void;
  onVerDetalles: (escaneo: EscaneoListadoType) => void;
  onMover: (escaneo: EscaneoListadoType) => void;
  onEliminar: (escaneo: EscaneoListadoType) => void;
  escaneoDetalle: EscaneoResultResponse | null;
  loadingDetalle: boolean;
  onCerrarDetalle: () => void;
  escaneoMover: EscaneoListadoType | null;
  auditorias: AuditoriaResponse[];
  nuevaAuditoriaId: string;
  onChangeAuditoria: (id: string) => void;
  onConfirmarMover: () => void;
  onCancelarMover: () => void;
  moving: boolean;
}

function formatDate(value: string | null): string {
  if (!value) return "—";
  return new Date(value).toLocaleString("es-AR");
}

export default function EscaneosListado({
  escaneos,
  page,
  totalPages,
  onAnterior,
  onSiguiente,
  onVerDetalles,
  onMover,
  onEliminar,
  escaneoDetalle,
  loadingDetalle,
  onCerrarDetalle,
  escaneoMover,
  auditorias,
  nuevaAuditoriaId,
  onChangeAuditoria,
  onConfirmarMover,
  onCancelarMover,
  moving,
}: Props) {
  if (escaneos.length === 0) {
    return <div className="empty-state">No hay escaneos registrados.</div>;
  }

  return (
    <>
      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Auditoría</th>
              <th>Objetivos</th>
              <th>Estado</th>
              <th>Progreso</th>
              <th>Nmap</th>
              <th>Iniciado</th>
              <th>Completado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {escaneos.map((escaneo) => (
              <tr key={escaneo.escaneoId}>
                <td className="cell-primary">{escaneo.auditoriaNombre}</td>
                <td>
                  <div className="escaneos-objetivos">
                    {escaneo.objetivos.map((objetivo) => (
                      <span key={objetivo}>{objetivo}</span>
                    ))}
                  </div>
                </td>
                <td>
                  <span className={`status status-${escaneo.estado}`}>
                    {escaneo.estado}
                  </span>
                </td>
                <td>{escaneo.progreso}%</td>
                <td className="cell-mono">{escaneo.nmapVersion ?? "—"}</td>
                <td>{formatDate(escaneo.iniciadoA)}</td>
                <td>{formatDate(escaneo.completadoA)}</td>
                <td>
                  <div className="escaneos-actions">
                    <button
                      type="button"
                      className="button"
                      onClick={() => onVerDetalles(escaneo)}
                    >
                      Ver detalles
                    </button>
                    <button
                      type="button"
                      className="button"
                      onClick={() => onMover(escaneo)}
                      disabled={escaneo.estado === "EN_PROCESO"}
                    >
                      Mover
                    </button>
                    <button
                      type="button"
                      className="button"
                      onClick={() => onEliminar(escaneo)}
                    >
                      Eliminar
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="listado-pagination">
        <button
          type="button"
          className="button"
          onClick={onAnterior}
          disabled={page === 0}
        >
          Anterior
        </button>
        <span>
          Página {page + 1} de {Math.max(totalPages, 1)}
        </span>
        <button
          type="button"
          className="button"
          onClick={onSiguiente}
          disabled={page >= totalPages - 1}
        >
          Siguiente
        </button>
      </div>

      {/* Modal detalle */}
      {/* {escaneoDetalle && (
        <div className="escaneo-modal-overlay" onClick={onCerrarDetalle}>
          <div
            className="escaneo-modal"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="escaneo-modal-header">
              <h2>Detalles del escaneo</h2>
              <button
                type="button"
                className="escaneo-modal-close"
                onClick={onCerrarDetalle}
                aria-label="Cerrar"
              >
                ×
              </button>
            </div>
            <div className="escaneo-modal-body">
              {loadingDetalle ? (
                <p>Cargando...</p>
              ) : (
                <pre className="console">
                  {JSON.stringify(escaneoDetalle.resultado, null, 2)}
                </pre>
              )}
            </div>
            <div className="escaneo-modal-footer">
              <button
                type="button"
                className="button"
                onClick={onCerrarDetalle}
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )} */}

      {/* Modal mover */}
      {escaneoMover && (
        <div className="escaneo-modal-overlay" onClick={onCancelarMover}>
          <div
            className="escaneo-modal"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="escaneo-modal-header">
              <h2>Mover escaneo</h2>
              <button
                type="button"
                className="escaneo-modal-close"
                onClick={onCancelarMover}
                aria-label="Cerrar"
              >
                ×
              </button>
            </div>
            <div className="escaneo-modal-body">
              <p className="escaneo-mover-actual">
                Auditoría actual:{" "}
                <strong>{escaneoMover.auditoriaNombre}</strong>
              </p>
              <div className="field">
                <label htmlFor="nueva-auditoria">Nueva auditoría</label>
                <select
                  id="nueva-auditoria"
                  value={nuevaAuditoriaId}
                  onChange={(e) => onChangeAuditoria(e.target.value)}
                >
                  {auditorias.map((auditoria) => (
                    <option key={auditoria.id} value={auditoria.id}>
                      {auditoria.nombre}
                    </option>
                  ))}
                </select>
              </div>
            </div>
            <div className="escaneo-modal-footer">
              <button
                type="button"
                className="button button-esc-m"
                onClick={onCancelarMover}
                disabled={moving}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="button button-esc-m"
                onClick={onConfirmarMover}
                disabled={
                  moving ||
                  !nuevaAuditoriaId ||
                  nuevaAuditoriaId === escaneoMover.auditoriaId
                }
              >
                {moving ? "Moviendo..." : "Mover"}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}