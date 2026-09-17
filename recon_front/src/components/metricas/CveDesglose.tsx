import {
  useCallback,
  useRef,
  useState,
} from "react";

import { fetchCveDetalle } from "@/data/vulnerabilidades";

import type {
  CveDesglose as CveDesgloseType,
  CveDetalle,
  CveResumen,
} from "@/data/types";

interface Props {
  data: CveDesgloseType | null;
  pageSize?: number;
}

/* ============================================================
 * Formatters
 * ============================================================ */

function formatearCvss(
  score: number | null,
) {
  return score != null
    ? score.toFixed(1)
    : "—";
}

function formatearEpss(
  score: number | null,
) {
  return score != null
    ? `${(score * 100).toFixed(2)}%`
    : "—";
}

function formatearFecha(
  value: string | null,
) {
  return value
    ? new Date(value).toLocaleString(
        "es-AR",
      )
    : "—";
}

/* ============================================================
 * Ranking
 * ============================================================ */

function CveRanking({
  items,
  emptyMessage,
  pageSize,
  details,
  loadingCves,
  errors,
  loadDetail,
}: {
  items: CveResumen[];
  emptyMessage: string;
  pageSize: number;

  details: Record<
    string,
    CveDetalle
  >;

  loadingCves: Set<string>;

  errors: Record<
    string,
    string
  >;

  loadDetail: (
    cveId: string,
  ) => Promise<void>;
}) {
  const [page, setPage] =
    useState(0);

  const [
    expandedId,
    setExpandedId,
  ] = useState<
    string | null
  >(null);

  const totalPages =
    Math.max(
      1,
      Math.ceil(
        items.length /
          pageSize,
      ),
    );

  const safePage =
    Math.min(
      page,
      totalPages - 1,
    );

  const start =
    safePage * pageSize;

  const visible =
    items.slice(
      start,
      start + pageSize,
    );

  if (
    items.length === 0
  ) {
    return (
      <div className="dashboard-empty">
        <p>
          {emptyMessage}
        </p>
      </div>
    );
  }

  return (
    <div className="security-ranking-wrapper">
      <ol className="security-ranking">
        {visible.map(
          (cve, index) => {
            const expanded =
              expandedId ===
              cve.cveId;

            const detail =
              details[
                cve.cveId
              ];

            const loading =
              loadingCves.has(
                cve.cveId,
              );

            const error =
              errors[
                cve.cveId
              ];

            const exploitRefs =
              detail
                ?.exploitRefs ??
              [];

            const cwes =
              cve.cwes ?? [];

            return (
              <li
                key={
                  cve.cveId
                }
                className={`security-ranking-item ${
                  expanded
                    ? "security-ranking-item-expanded"
                    : ""
                }`}
              >
                <button
                  type="button"
                  className="security-ranking-trigger"
                  aria-expanded={
                    expanded
                  }
                  onClick={() => {
                    if (
                      expanded
                    ) {
                      setExpandedId(
                        null,
                      );

                      return;
                    }

                    setExpandedId(
                      cve.cveId,
                    );

                    void loadDetail(
                      cve.cveId,
                    );
                  }}
                >
                  <div className="security-ranking-position">
                    #
                    {start +
                      index +
                      1}
                  </div>

                  <div className="security-ranking-summary">
                    <div className="security-ranking-title">
                      <strong>
                        {
                          cve.cveId
                        }
                      </strong>

                      {cve.explotacionActiva && (
                        <span className="cve-badge cve-badge-kev">
                          KEV
                        </span>
                      )}
                    </div>

                    <div className="security-ranking-preview">
                      <span>
                        CVSS{" "}
                        <strong>
                          {formatearCvss(
                            cve.cvssScore,
                          )}
                        </strong>
                      </span>

                      <span>
                        EPSS{" "}
                        <strong>
                          {formatearEpss(
                            cve.epssScore,
                          )}
                        </strong>
                      </span>

                      <span>
                        Frecuencia{" "}
                        <strong>
                          {
                            cve.frecuencia
                          }
                        </strong>
                      </span>
                    </div>
                  </div>

                  <span
                    className={`security-ranking-chevron ${
                      expanded
                        ? "security-ranking-chevron-open"
                        : ""
                    }`}
                    aria-hidden="true"
                  >
                    ▾
                  </span>
                </button>

                {expanded && (
                  <div className="security-ranking-detail">
                    {loading &&
                      !detail && (
                        <div className="security-detail-loading">
                          Cargando
                          detalle de{" "}
                          {
                            cve.cveId
                          }
                          ...
                        </div>
                      )}

                    {error &&
                      !detail && (
                        <div className="security-detail-error">
                          <span>
                            {
                              error
                            }
                          </span>

                          <button
                            type="button"
                            className="button"
                            onClick={() => {
                              void loadDetail(
                                cve.cveId,
                              );
                            }}
                          >
                            Reintentar
                          </button>
                        </div>
                      )}

                    {detail && (
                      <div className="cve-detail">
                        <div className="cve-detail-header">
                          <div>
                            <span className="cve-detail-label">
                              Vulnerabilidad
                            </span>

                            <strong className="cve-detail-id">
                              {
                                detail.cveId
                              }
                            </strong>
                          </div>

                          <div className="cve-detail-badges">
                            {detail.severidad && (
                              <span className="cve-detail-badge">
                                {
                                  detail.severidad
                                }
                              </span>
                            )}

                            {detail.explotacionActiva && (
                              <span className="cve-badge cve-badge-kev">
                                CISA
                                KEV
                              </span>
                            )}
                          </div>
                        </div>

                        <div className="cve-detail-description">
                          <span className="cve-detail-label">
                            Descripción
                          </span>

                          <p>
                            {detail.descripcion ??
                              "Sin descripción disponible."}
                          </p>
                        </div>

                        <dl className="security-detail-grid cve-detail-metrics">
                          <div>
                            <dt>
                              CVSS
                            </dt>

                            <dd>
                              {formatearCvss(
                                detail.cvssScore,
                              )}
                            </dd>
                          </div>

                          <div>
                            <dt>
                              EPSS
                            </dt>

                            <dd>
                              {formatearEpss(
                                detail.epssScore,
                              )}
                            </dd>
                          </div>

                          <div>
                            <dt>
                              Frecuencia
                            </dt>

                            <dd>
                              {
                                cve.frecuencia
                              }
                            </dd>
                          </div>

                          <div>
                            <dt>
                              Explotación
                              conocida
                            </dt>

                            <dd>
                              {detail.explotacionActiva
                                ? "Sí · CISA KEV"
                                : "No registrada en KEV"}
                            </dd>
                          </div>

                          <div className="security-detail-wide">
                            <dt>
                              Vector
                              CVSS
                            </dt>

                            <dd className="cve-detail-vector">
                              {detail.cvssVector ??
                                "—"}
                            </dd>
                          </div>

                          <div className="security-detail-wide">
                            <dt>
                              CWE
                            </dt>

                            <dd>
                              {cwes.length >
                              0 ? (
                                <div className="security-tags">
                                  {cwes.map(
                                    (
                                      cwe,
                                    ) => (
                                      <span
                                        key={
                                          cwe
                                        }
                                        className="security-tag"
                                      >
                                        {
                                          cwe
                                        }
                                      </span>
                                    ),
                                  )}
                                </div>
                              ) : (
                                "Sin CWE asociado"
                              )}
                            </dd>
                          </div>
                        </dl>

                        <div className="cve-detail-section">
                          <h4>
                            Versiones
                            y
                            corrección
                          </h4>

                          <dl className="security-detail-grid">
                            <div className="security-detail-wide">
                              <dt>
                                Versiones
                                vulnerables
                              </dt>

                              <dd>
                                {detail.versionesVulnerables ??
                                  "Sin información"}
                              </dd>
                            </div>

                            <div>
                              <dt>
                                Versión
                                corregida
                              </dt>

                              <dd>
                                {detail.versionParche ??
                                  "Sin información"}
                              </dd>
                            </div>

                            <div>
                              <dt>
                                Tipo
                                de
                                corrección
                              </dt>

                              <dd>
                                {detail.tipoParche ??
                                  "Sin información"}
                              </dd>
                            </div>
                          </dl>
                        </div>

                        <div className="cve-detail-section">
                          <h4>
                            Mitigación
                          </h4>

                          <p className="cve-detail-text">
                            {detail.mitigacion ??
                              "No se dispone de información de mitigación."}
                          </p>
                        </div>

                        <div className="cve-detail-section">
                          <h4>
                            Referencias
                            de
                            explotación
                          </h4>

                          {exploitRefs.length >
                          0 ? (
                            <ul className="cve-detail-links">
                              {exploitRefs.map(
                                (
                                  reference,
                                ) => (
                                  <li
                                    key={
                                      reference
                                    }
                                  >
                                    <a
                                      href={
                                        reference
                                      }
                                      target="_blank"
                                      rel="noreferrer"
                                    >
                                      {
                                        reference
                                      }
                                    </a>
                                  </li>
                                ),
                              )}
                            </ul>
                          ) : (
                            <p className="cve-detail-empty">
                              Sin
                              referencias
                              de
                              explotación
                              registradas.
                            </p>
                          )}
                        </div>

                        <div className="cve-detail-section">
                          <h4>
                            Información
                            NVD
                          </h4>

                          <dl className="security-detail-grid">
                            <div>
                              <dt>
                                Publicada
                              </dt>

                              <dd>
                                {formatearFecha(
                                  detail.fechaPublicacion,
                                )}
                              </dd>
                            </div>

                            <div>
                              <dt>
                                Última
                                modificación
                              </dt>

                              <dd>
                                {formatearFecha(
                                  detail.ultimaModificacion,
                                )}
                              </dd>
                            </div>

                            {detail.nistUrl && (
                              <div className="security-detail-wide">
                                <dt>
                                  NIST
                                  NVD
                                </dt>

                                <dd>
                                  <a
                                    href={
                                      detail.nistUrl
                                    }
                                    target="_blank"
                                    rel="noreferrer"
                                    className="cve-detail-nist-link"
                                  >
                                    Ver
                                    registro
                                    oficial
                                  </a>
                                </dd>
                              </div>
                            )}
                          </dl>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </li>
            );
          },
        )}
      </ol>

      {items.length >
        pageSize && (
        <Pagination
          page={safePage}
          totalPages={
            totalPages
          }
          onPrevious={() =>
            setPage(
              (value) =>
                Math.max(
                  0,
                  value - 1,
                ),
            )
          }
          onNext={() =>
            setPage(
              (value) =>
                Math.min(
                  totalPages -
                    1,
                  value + 1,
                ),
            )
          }
        />
      )}
    </div>
  );
}

/* ============================================================
 * Pagination
 * ============================================================ */

function Pagination({
  page,
  totalPages,
  onPrevious,
  onNext,
}: {
  page: number;
  totalPages: number;
  onPrevious: () => void;
  onNext: () => void;
}) {
  return (
    <div className="listado-pagination security-pagination">
      <button
        type="button"
        className="button button-page"
        onClick={
          onPrevious
        }
        disabled={
          page === 0
        }
      >
        Anterior
      </button>

      <span>
        Página {page + 1} de{" "}
        {totalPages}
      </span>

      <button
        type="button"
        className="button button-page"
        onClick={onNext}
        disabled={
          page >=
          totalPages - 1
        }
      >
        Siguiente
      </button>
    </div>
  );
}

/* ============================================================
 * Component
 * ============================================================ */

export function CveDesglose({
  data,
  pageSize = 5,
}: Props) {
  const [
    cveDetails,
    setCveDetails,
  ] = useState<
    Record<
      string,
      CveDetalle
    >
  >({});

  const [
    loadingCves,
    setLoadingCves,
  ] = useState<
    Set<string>
  >(() => new Set());

  const [
    cveErrors,
    setCveErrors,
  ] = useState<
    Record<
      string,
      string
    >
  >({});

  const cveLoadingRef =
    useRef<
      Set<string>
    >(new Set());

  const loadCveDetail =
    useCallback(
      async (
        cveId: string,
      ) => {
        if (
          cveDetails[
            cveId
          ] ||
          cveLoadingRef.current.has(
            cveId,
          )
        ) {
          return;
        }

        cveLoadingRef.current.add(
          cveId,
        );

        setLoadingCves(
          (current) => {
            const next =
              new Set(
                current,
              );

            next.add(
              cveId,
            );

            return next;
          },
        );

        setCveErrors(
          (current) => {
            const next = {
              ...current,
            };

            delete next[
              cveId
            ];

            return next;
          },
        );

        try {
          const detail =
            await fetchCveDetalle(
              cveId,
            );

          setCveDetails(
            (current) => ({
              ...current,
              [cveId]:
                detail,
            }),
          );
        } catch (error) {
          setCveErrors(
            (current) => ({
              ...current,

              [cveId]:
                error instanceof
                Error
                  ? error.message
                  : "No se pudo cargar el detalle.",
            }),
          );
        } finally {
          cveLoadingRef.current.delete(
            cveId,
          );

          setLoadingCves(
            (current) => {
              const next =
                new Set(
                  current,
                );

              next.delete(
                cveId,
              );

              return next;
            },
          );
        }
      },
      [cveDetails],
    );

  return (
    <>
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Vulnerabilidades
        </p>

        <h2 id="security-breakdown-title">
          Desglose de CVEs
        </h2>

        <p>
          Vulnerabilidades
          priorizadas por
          criticidad,
          probabilidad de
          explotación,
          frecuencia y
          presencia en CISA
          KEV.
        </p>
      </div>

      <div className="security-breakdown-grid">
        <article className="dashboard-card security-breakdown-card">
          <h3>
            Mayor criticidad
          </h3>

          <CveRanking
            items={
              data?.mayorCriticidad ??
              []
            }
            emptyMessage="Todavía no hay datos de criticidad."
            pageSize={
              pageSize
            }
            details={
              cveDetails
            }
            loadingCves={
              loadingCves
            }
            errors={
              cveErrors
            }
            loadDetail={
              loadCveDetail
            }
          />
        </article>

        <article className="dashboard-card security-breakdown-card">
          <h3>
            Mayor probabilidad
            de explotación
          </h3>

          <CveRanking
            items={
              data?.mayorProbabilidadExplotacion ??
              []
            }
            emptyMessage="Todavía no hay datos de EPSS."
            pageSize={
              pageSize
            }
            details={
              cveDetails
            }
            loadingCves={
              loadingCves
            }
            errors={
              cveErrors
            }
            loadDetail={
              loadCveDetail
            }
          />
        </article>

        <article className="dashboard-card security-breakdown-card">
          <h3>
            Más frecuentes
          </h3>

          <CveRanking
            items={
              data?.masComunes ??
              []
            }
            emptyMessage="Todavía no hay datos de frecuencia."
            pageSize={
              pageSize
            }
            details={
              cveDetails
            }
            loadingCves={
              loadingCves
            }
            errors={
              cveErrors
            }
            loadDetail={
              loadCveDetail
            }
          />
        </article>

        <article className="dashboard-card security-breakdown-card">
          <h3>
            Explotación
            conocida
          </h3>

          <CveRanking
            items={
              data?.explotacionActiva ??
              []
            }
            emptyMessage="No hay vulnerabilidades presentes en CISA KEV."
            pageSize={
              pageSize
            }
            details={
              cveDetails
            }
            loadingCves={
              loadingCves
            }
            errors={
              cveErrors
            }
            loadDetail={
              loadCveDetail
            }
          />
        </article>
      </div>
    </>
  );
}