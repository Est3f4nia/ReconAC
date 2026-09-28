import {
  Fragment,
  useState,
} from "react";

import { fetchEscaneo } from "@/data/escaneos";

import type {
  EjecucionHistorial,
  EscaneoResultResponse,
} from "@/data/types";

import "@/components/metricas/styles/ScanHistory.css";

interface Props {
  auditoriaId: string;
  escaneos: EjecucionHistorial[];
  pageSize?: number;
}

function formatearFecha(
  value: string | null | undefined,
) {
  return value
    ? new Date(value).toLocaleString()
    : "—";
}

function formatearCvss(
  value: number | null,
) {
  return value != null
    ? value.toFixed(1)
    : "—";
}

function formatearObjetivos(
  objetivos: string[],
) {
  if (!objetivos?.length) {
    return "—";
  }

  return objetivos.join(", ");
}

export function ScanHistory({
  auditoriaId,
  escaneos,
  pageSize = 6,
}: Props) {
  const [page, setPage] =
    useState(0);

  const [abierto, setAbierto] =
    useState<string | null>(null);

  const [resultados, setResultados] =
    useState<
      Record<
        string,
        EscaneoResultResponse
      >
    >({});

  const [cargando, setCargando] =
    useState<string | null>(null);

  const [errores, setErrores] =
    useState<
      Record<string, string>
    >({});

  const totalPages = Math.max(
    1,
    Math.ceil(
      escaneos.length / pageSize,
    ),
  );

  const safePage = Math.min(
    page,
    totalPages - 1,
  );

  const start =
    safePage * pageSize;

  const visibles = escaneos.slice(
    start,
    start + pageSize,
  );

  async function toggleResultado(
    escaneo: EjecucionHistorial,
  ) {
    const escaneoId =
      escaneo.escaneoId;

    if (abierto === escaneoId) {
      setAbierto(null);
      return;
    }

    setAbierto(escaneoId);

    /*
     * Solamente existe resultado completo
     * para una ejecución completada.
     */
    if (
      escaneo.estado !== "COMPLETADO"
    ) {
      return;
    }

    /*
     * Si ya fue cargado, reutilizamos
     * el resultado.
     */
    if (resultados[escaneoId]) {
      return;
    }

    setErrores(
      (current) => ({
        ...current,
        [escaneoId]: "",
      }),
    );

    try {
      setCargando(escaneoId);

      const resultado =
        await fetchEscaneo(
          auditoriaId,
          escaneoId,
        );

      setResultados(
        (current) => ({
          ...current,
          [escaneoId]:
            resultado,
        }),
      );
    } catch (error) {
      setErrores(
        (current) => ({
          ...current,

          [escaneoId]:
            error instanceof Error
              ? error.message
              : "No se pudo cargar el resultado.",
        }),
      );
    } finally {
      setCargando(
        (current) =>
          current === escaneoId
            ? null
            : current,
      );
    }
  }

  return (
    <section
      className="dashboard-section scan-history-section"
      aria-labelledby="scan-history-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Trazabilidad
        </p>

        <h2 id="scan-history-title">
          Historial de escaneos
        </h2>

        <p>
          Ejecuciones registradas y
          resultados obtenidos por ReconAC.
        </p>
      </div>

      {escaneos.length === 0 ? (
        <div className="dashboard-empty">
          <p>
            Todavía no hay ejecuciones
            registradas.
          </p>
        </div>
      ) : (
        <>
          <div className="scan-history-table-wrapper">
            <table className="scan-history-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Estado</th>
                  <th>Objetivos</th>
                  <th>Activos</th>
                  <th>Puertos</th>
                  <th>CVEs</th>
                  <th>Críticas</th>
                  <th>CVSS</th>
                  <th>Progreso</th>
                  <th aria-label="Detalle" />
                </tr>
              </thead>

              <tbody>
                {visibles.map(
                  (escaneo) => {
                    const resultado =
                      resultados[
                        escaneo.escaneoId
                      ];

                    const error =
                      errores[
                        escaneo.escaneoId
                      ];

                    const estaAbierto =
                      abierto ===
                      escaneo.escaneoId;

                    return (
                      <Fragment
                        key={
                          escaneo.escaneoId
                        }
                      >
                        <tr
                          className={`scan-history-row ${
                            estaAbierto
                              ? "scan-history-row-open"
                              : ""
                          }`}
                          tabIndex={0}
                          aria-expanded={
                            estaAbierto
                          }
                          onClick={() => {
                            void toggleResultado(
                              escaneo,
                            );
                          }}
                          onKeyDown={(
                            event,
                          ) => {
                            if (
                              event.key ===
                                "Enter" ||
                              event.key === " "
                            ) {
                              event.preventDefault();

                              void toggleResultado(
                                escaneo,
                              );
                            }
                          }}
                        >
                          <td>
                            <div className="scan-history-date">
                              <strong>
                                {formatearFecha(
                                  escaneo.fecha,
                                )}
                              </strong>

                              <span>
                                {escaneo.escaneoId.slice(
                                  0,
                                  8,
                                )}
                              </span>
                            </div>
                          </td>

                          <td>
                            <span
                              className={`status status-${escaneo.estado}`}
                            >
                              {escaneo.estado.replace(
                                "_",
                                " ",
                              )}
                            </span>
                          </td>

                          <td>
                            <span
                              className="scan-history-targets"
                              title={formatearObjetivos(
                                escaneo.objetivos,
                              )}
                            >
                              {formatearObjetivos(
                                escaneo.objetivos,
                              )}
                            </span>
                          </td>

                          <td>
                            {escaneo.activos}
                          </td>

                          <td>
                            {escaneo.puertos}
                          </td>

                          <td>
                            {escaneo.cves}
                          </td>

                          <td>
                            {
                              escaneo.cvesCriticos
                            }
                          </td>

                          <td>
                            {formatearCvss(
                              escaneo.cvssPromedio,
                            )}
                          </td>

                          <td>
                            {escaneo.progreso}%
                          </td>

                          <td className="scan-history-action-cell">
                            <button
                              type="button"
                              className="scan-history-detail-button"
                              aria-label={
                                estaAbierto
                                  ? "Cerrar detalle"
                                  : "Ver detalle"
                              }
                              onClick={(
                                event,
                              ) => {
                                event.stopPropagation();

                                void toggleResultado(
                                  escaneo,
                                );
                              }}
                            >
                              <span
                                className={`scan-history-chevron ${
                                  estaAbierto
                                    ? "scan-history-chevron-open"
                                    : ""
                                }`}
                              >
                                ▾
                              </span>
                            </button>
                          </td>
                        </tr>

                        {estaAbierto && (
                          <tr className="scan-history-detail-row">
                            <td colSpan={10}>
                              <div className="scan-history-detail">
                                {escaneo.mensajeError && (
                                  <div
                                    className="dashboard-error"
                                    role="alert"
                                  >
                                    {
                                      escaneo.mensajeError
                                    }
                                  </div>
                                )}

                                {escaneo.estado !==
                                  "COMPLETADO" &&
                                  !escaneo.mensajeError && (
                                    <div className="dashboard-empty scan-history-detail-empty">
                                      <p>
                                        El resultado
                                        completo estará
                                        disponible cuando
                                        finalice el escaneo.
                                      </p>
                                    </div>
                                  )}

                                {cargando ===
                                  escaneo.escaneoId && (
                                  <div className="scan-history-loading">
                                    Cargando resultado...
                                  </div>
                                )}

                                {error && (
                                  <div
                                    className="dashboard-error"
                                    role="alert"
                                  >
                                    {error}
                                  </div>
                                )}

                                {resultado && (
                                  <>
                                    {/* ==============================
                                        Datos generales
                                        ============================== */}

                                    <div className="scan-history-detail-heading">
                                      <div>
                                        <span className="dashboard-section-eyebrow">
                                          Resultado
                                        </span>

                                        <h3>
                                          Escaneo{" "}
                                          {resultado.escaneoId.slice(
                                            0,
                                            8,
                                          )}
                                        </h3>
                                      </div>

                                      <span
                                        className={`status status-${resultado.estado}`}
                                      >
                                        {resultado.estado.replace(
                                          "_",
                                          " ",
                                        )}
                                      </span>
                                    </div>

                                    <dl className="scan-history-detail-grid">
                                      <div>
                                        <dt>
                                          Nmap
                                        </dt>

                                        <dd>
                                          {resultado.nmapVersion ??
                                            "—"}
                                        </dd>
                                      </div>

                                      <div>
                                        <dt>
                                          Progreso
                                        </dt>

                                        <dd>
                                          {
                                            resultado.progreso
                                          }
                                          %
                                        </dd>
                                      </div>

                                      <div>
                                        <dt>
                                          Inicio
                                        </dt>

                                        <dd>
                                          {formatearFecha(
                                            resultado.iniciadoA,
                                          )}
                                        </dd>
                                      </div>

                                      <div>
                                        <dt>
                                          Finalización
                                        </dt>

                                        <dd>
                                          {formatearFecha(
                                            resultado.completadoA,
                                          )}
                                        </dd>
                                      </div>

                                      <div>
                                        <dt>
                                          Activos
                                        </dt>

                                        <dd>
                                          {
                                            resultado.activos
                                              .length
                                          }
                                        </dd>
                                      </div>

                                      <div>
                                        <dt>
                                          Puertos
                                        </dt>

                                        <dd>
                                          {
                                            escaneo.puertos
                                          }
                                        </dd>
                                      </div>

                                      <div>
                                        <dt>
                                          CVEs
                                        </dt>

                                        <dd>
                                          {
                                            escaneo.cves
                                          }
                                        </dd>
                                      </div>

                                      <div>
                                        <dt>
                                          CVEs críticas
                                        </dt>

                                        <dd>
                                          {
                                            escaneo.cvesCriticos
                                          }
                                        </dd>
                                      </div>

                                      <div>
                                        <dt>
                                          CVSS promedio
                                        </dt>

                                        <dd>
                                          {formatearCvss(
                                            escaneo.cvssPromedio,
                                          )}
                                        </dd>
                                      </div>

                                      <div className="scan-history-detail-wide">
                                        <dt>
                                          Objetivos
                                        </dt>

                                        <dd>
                                          {formatearObjetivos(
                                            escaneo.objetivos,
                                          )}
                                        </dd>
                                      </div>
                                    </dl>

                                    {/* ==============================
                                        Activos
                                        ============================== */}

                                    <div className="scan-history-assets">
                                      <div className="scan-history-assets-heading">
                                        <h4>
                                          Activos detectados
                                        </h4>

                                        <span>
                                          {
                                            resultado.activos
                                              .length
                                          }
                                        </span>
                                      </div>

                                      {resultado.activos
                                        .length === 0 ? (
                                        <p className="scan-history-no-data">
                                          No se detectaron
                                          activos.
                                        </p>
                                      ) : (
                                        resultado.activos.map(
                                          (
                                            activo,
                                          ) => (
                                            <article
                                              key={
                                                activo.activoId
                                              }
                                              className="scan-history-asset"
                                            >
                                              <div className="scan-history-asset-header">
                                                <div>
                                                  <strong>
                                                    {
                                                      activo.host
                                                    }
                                                  </strong>

                                                  <span>
                                                    {activo.hostname ??
                                                      "Sin hostname"}
                                                  </span>
                                                </div>

                                                <span className="scan-history-port-count">
                                                  {
                                                    activo.puertos
                                                      .length
                                                  }{" "}
                                                  {activo.puertos
                                                    .length ===
                                                  1
                                                    ? "puerto"
                                                    : "puertos"}
                                                </span>
                                              </div>

                                              <dl className="scan-history-asset-meta">
                                                <div>
                                                  <dt>
                                                    SO
                                                  </dt>

                                                  <dd>
                                                    {activo.so ??
                                                      "—"}
                                                  </dd>
                                                </div>

                                                <div>
                                                  <dt>
                                                    Probabilidad
                                                    SO
                                                  </dt>

                                                  <dd>
                                                    {activo.soProbab !=
                                                    null
                                                      ? `${activo.soProbab}%`
                                                      : "—"}
                                                  </dd>
                                                </div>

                                                <div>
                                                  <dt>
                                                    MAC
                                                  </dt>

                                                  <dd>
                                                    {activo.mac ??
                                                      "—"}
                                                  </dd>
                                                </div>
                                              </dl>

                                              {activo.puertos
                                                .length >
                                              0 ? (
                                                <div className="scan-history-ports-wrapper">
                                                  <table className="scan-history-ports-table">
                                                    <thead>
                                                      <tr>
                                                        <th>
                                                          Puerto
                                                        </th>
                                                        <th>
                                                          Estado
                                                        </th>
                                                        <th>
                                                          Servicio
                                                        </th>
                                                        <th>
                                                          Producto
                                                        </th>
                                                        <th>
                                                          Versión
                                                        </th>
                                                        <th>
                                                          Información
                                                        </th>
                                                        <th>
                                                          CPE
                                                        </th>
                                                      </tr>
                                                    </thead>

                                                    <tbody>
                                                      {activo.puertos.map(
                                                        (
                                                          puerto,
                                                        ) => (
                                                          <tr
                                                            key={`${activo.activoId}-${puerto.numero}-${puerto.protocolo}`}
                                                          >
                                                            <td>
                                                              <strong>
                                                                {
                                                                  puerto.numero
                                                                }
                                                                /
                                                                {
                                                                  puerto.protocolo
                                                                }
                                                              </strong>
                                                            </td>

                                                            <td>
                                                              {
                                                                puerto.estado
                                                              }
                                                            </td>

                                                            <td>
                                                              {puerto.servicio ??
                                                                "—"}
                                                            </td>

                                                            <td>
                                                              {puerto.producto ??
                                                                "—"}
                                                            </td>

                                                            <td>
                                                              {puerto.version ??
                                                                "—"}
                                                            </td>

                                                            <td>
                                                              {puerto.extrainfo ??
                                                                "—"}
                                                            </td>

                                                            <td>
                                                              {(puerto.cpes ??
                                                                [])
                                                                .length ===
                                                              0 ? (
                                                                "—"
                                                              ) : (
                                                                <div className="scan-history-cpes">
                                                                  {(
                                                                    puerto.cpes ??
                                                                    []
                                                                  ).map(
                                                                    (
                                                                      cpe,
                                                                    ) => (
                                                                      <code
                                                                        key={
                                                                          cpe
                                                                        }
                                                                      >
                                                                        {
                                                                          cpe
                                                                        }
                                                                      </code>
                                                                    ),
                                                                  )}
                                                                </div>
                                                              )}
                                                            </td>
                                                          </tr>
                                                        ),
                                                      )}
                                                    </tbody>
                                                  </table>
                                                </div>
                                              ) : (
                                                <p className="scan-history-no-data">
                                                  Sin puertos
                                                  detectados.
                                                </p>
                                              )}
                                            </article>
                                          ),
                                        )
                                      )}
                                    </div>
                                  </>
                                )}
                              </div>
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    );
                  },
                )}
              </tbody>
            </table>
          </div>

          {escaneos.length >
            pageSize && (
            <nav
              className="listado-pagination"
              aria-label="Páginas del historial de escaneos"
            >
              <button
                type="button"
                className="button button-page"
                onClick={() =>
                  setPage((current) =>
                    Math.max(
                      0,
                      current - 1,
                    ),
                  )
                }
                disabled={
                  safePage === 0
                }
              >
                Anterior
              </button>

              <span>
                Página {safePage + 1}{" "}
                de {totalPages} ·{" "}
                {escaneos.length}{" "}
                escaneos
              </span>

              <button
                type="button"
                className="button button-page"
                onClick={() =>
                  setPage((current) =>
                    Math.min(
                      totalPages - 1,
                      current + 1,
                    ),
                  )
                }
                disabled={
                  safePage >=
                  totalPages - 1
                }
              >
                Siguiente
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  );
}