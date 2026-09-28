import type { EscaneoListado as EscaneoListadoType, EscaneoResultResponse, AuditoriaResponse } from "@/data/types";
import { useState } from "react";
import { X } from "lucide-react";
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
  
  const [ escaneoEliminar, setEscaneoEliminar ] = useState<EscaneoListadoType | null>(null);
  const [error] = useState("");

  if (escaneos.length === 0) {
    return (
      <div className="empty-state">
        No hay escaneos registrados.
      </div>
    );
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
                      className="button button-danger"
                      onClick={() =>
                        setEscaneoEliminar(escaneo)
                      }
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

      {escaneoDetalle && (
        <div
          className="escaneo-modal-overlay"
          onClick={onCerrarDetalle}
        >
          <div
            className="escaneo-modal"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="escaneo-modal-header">
              <h2>Detalles del escaneo</h2>

              <button
                type="button"
                className="modal-close"
                onClick={onCerrarDetalle}
                aria-label="Cerrar"
              >
                <X size={18} />
              </button>
            </div>

            <div className="escaneo-modal-body">
              {loadingDetalle ? (
                <p>Cargando...</p>
              ) : (
                <>
                  <div className="scan-delete-summary">
                    <span>
                      Estado: <strong>{escaneoDetalle.estado}</strong>
                    </span>

                    <span>
                      Progreso: <strong>{escaneoDetalle.progreso}%</strong>
                    </span>

                    <span>
                      Nmap:{" "}
                      <strong>{escaneoDetalle.nmapVersion ?? "—"}</strong>
                    </span>

                    <span>
                      Iniciado:{" "}
                      <strong>{formatDate(escaneoDetalle.iniciadoA)}</strong>
                    </span>

                    <span>
                      Completado:{" "}
                      <strong>{formatDate(escaneoDetalle.completadoA)}</strong>
                    </span>

                    <span>
                      Activos:{" "}
                      <strong>{escaneoDetalle.activos.length}</strong>
                    </span>
                  </div>

                  <pre className="console">
                    {JSON.stringify(escaneoDetalle.activos, null, 2)}
                  </pre>
                </>
              )}
            </div>

            <div className="escaneo-modal-footer">
              <button
                type="button"
                className="button button-modal-cerrar"
                onClick={onCerrarDetalle}
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

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
                className="modal-close"
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

      {escaneoEliminar && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={() =>
            setEscaneoEliminar(null)
          }
        >
          <div
            className="auditoria-delete-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-scan-title"
            onMouseDown={(event) =>
              event.stopPropagation()
            }
          >
            <div className="modal-header">
              <div>
                <p className="modal-section-eyebrow">
                  Confirmación
                </p>

                <h2 id="delete-scan-title">
                  Eliminar escaneo
                </h2>
              </div>

              <button
                type="button"
                className="button"
                aria-label="Cerrar"
                onClick={() =>
                  setEscaneoEliminar(null)
                }
              >
                <X size={18} />
              </button>
            </div>

            <div className="modal-body">
              <p>
                Vas a eliminar: escaneo{" "}
                <strong>
                  {escaneoEliminar.escaneoId.slice(
                    0,
                    8,
                  )}
                </strong>
                .
              </p>

              <p>
                Se eliminarán también los
                activos, puertos y asociaciones
                dependientes de esta ejecución.
              </p>

              <div className="modal-delete-warning">
                <p>
                Esta acción es irreversible.
                </p>
              </div>

              <div className="scan-delete-summary">
                <span>
                  Auditoría:{" "}
                  <strong>
                    {
                      escaneoEliminar.auditoriaNombre
                    }
                  </strong>
                </span>

                <span>
                  Fecha:{" "}
                  <strong>
                    {formatDate(
                      escaneoEliminar.iniciadoA,
                    )}
                  </strong>
                </span>

                <span>
                  Estado:{" "}
                  <strong>
                    {escaneoEliminar.estado}
                  </strong>
                </span>

                <span>
                  Objetivos:{" "}
                  <strong>
                    {escaneoEliminar.objetivos.join(
                      ", ",
                    )}
                  </strong>
                </span>
              </div>
              
              {error && (
                <div
                  className="dashboard-error"
                  role="alert"
                >
                  {error}
                </div>
              )}

            </div>

            <div className="modal-actions">
              <button
                type="button"
                className="button button-audit-header"
                onClick={() =>
                  setEscaneoEliminar(null)
                }
              >
                Cancelar
              </button>

              <button
                type="button"
                className="button button-danger"
                onClick={() => {
                  onEliminar(
                    escaneoEliminar,
                  );

                  setEscaneoEliminar(null);
                }}
              >
                Eliminar escaneo
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}