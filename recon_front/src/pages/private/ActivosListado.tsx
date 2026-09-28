import type { ActivoAgrupadoResponse } from "@/data/types";
import "@/pages/private/styles/ActivosListado.css";

interface Props {
  activos: ActivoAgrupadoResponse[];
  page: number;
  totalPages: number;
  onAnterior: () => void;
  onSiguiente: () => void;
  onVerDetalle: (activo: ActivoAgrupadoResponse) => void;
  activoDetalle: ActivoAgrupadoResponse | null;
  onCerrarDetalle: () => void;
}

export default function ActivosListado({
  activos,
  page,
  totalPages,
  onAnterior,
  onSiguiente,
  onVerDetalle,
  activoDetalle,
  onCerrarDetalle,
}: Props) {
  if (activos.length === 0) {
    return (
      <div className="empty-state">
        No hay activos registrados.
      </div>
    );
  }

  return (
    <>
      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Host</th>
              <th>Hostname</th>
              <th>Sistema operativo</th>
              <th>Probabilidad</th>
              <th>MAC</th>
              <th>Descripción</th>
              <th>Escaneos</th>
              <th>Acciones</th>
            </tr>
          </thead>

          <tbody>
            {activos.map((activo, index) => {
              const escaneoIds =
                activo.escaneoIds ?? [];

              const key = [
                activo.host,
                activo.hostname,
                activo.so,
                ...escaneoIds,
                index,
              ].join("|");

              return (
                <tr key={key}>
                  <td className="cell-mono">
                    {activo.host ?? "—"}
                  </td>

                  <td>
                    {activo.hostname ?? "—"}
                  </td>

                  <td>
                    {activo.so ?? "—"}
                  </td>

                  <td>
                    {activo.soProbab != null
                      ? `${activo.soProbab}%`
                      : "—"}
                  </td>

                  <td className="cell-mono">
                    {activo.mac ?? "—"}
                  </td>

                  <td>
                    {activo.descripcion ?? "—"}
                  </td>

                  <td>
                    {escaneoIds.length}
                  </td>

                  <td>
                    <button
                      type="button"
                      className="button button-activoP"
                      onClick={() =>
                        onVerDetalle(activo)
                      }
                    >
                      Ver
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="listado-pagination">
        <button
          type="button"
          className="button button-page"
          onClick={onAnterior}
          disabled={page === 0}
        >
          Anterior
        </button>

        <span>
          Página {page + 1} de{" "}
          {Math.max(totalPages, 1)}
        </span>

        <button
          type="button"
          className="button button-page"
          onClick={onSiguiente}
          disabled={
            totalPages === 0 ||
            page >= totalPages - 1
          }
        >
          Siguiente
        </button>
      </div>

      {activoDetalle && (
        <div
          className="activo-modal-overlay"
          onClick={onCerrarDetalle}
        >
          <div
            className="activo-modal"
            onClick={(event) =>
              event.stopPropagation()
            }
          >
            <div className="activo-modal-header">
              <h2>Detalle del activo</h2>

              <button
                type="button"
                className="activo-modal-close"
                onClick={onCerrarDetalle}
                aria-label="Cerrar"
              >
                ×
              </button>
            </div>

            <div className="activo-detalle">
              <div className="activo-detalle-item">
                <strong>Host</strong>
                <code>
                  {activoDetalle.host ?? "—"}
                </code>
              </div>

              <div className="activo-detalle-item">
                <strong>Hostname</strong>
                <span>
                  {activoDetalle.hostname ?? "—"}
                </span>
              </div>

              <div className="activo-detalle-item">
                <strong>Sistema operativo</strong>
                <span>
                  {activoDetalle.so ?? "—"}
                </span>
              </div>

              <div className="activo-detalle-item">
                <strong>
                  Probabilidad del SO
                </strong>

                <span>
                  {activoDetalle.soProbab != null
                    ? `${activoDetalle.soProbab}%`
                    : "—"}
                </span>
              </div>

              <div className="activo-detalle-item">
                <strong>MAC</strong>
                <span>
                  {activoDetalle.mac ?? "—"}
                </span>
              </div>

              <div className="activo-detalle-item">
                <strong>Descripción</strong>
                <span>
                  {activoDetalle.descripcion ??
                    "—"}
                </span>
              </div>

              <div className="activo-detalle-escaneos">
                <strong>
                  Escaneos donde fue detectado
                </strong>

                {(activoDetalle.escaneoIds ?? [])
                  .length === 0 ? (
                  <span>
                    Sin escaneos registrados.
                  </span>
                ) : (
                  <div className="activo-escaneos-lista">
                    {(
                      activoDetalle.escaneoIds ??
                      []
                    ).map((escaneoId) => (
                      <code key={escaneoId}>
                        {escaneoId}
                      </code>
                    ))}
                  </div>
                )}
              </div>
            </div>

            <div className="activo-modal-footer" />
          </div>
        </div>
      )}
    </>
  );
}