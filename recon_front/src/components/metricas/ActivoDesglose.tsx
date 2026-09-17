import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";

import { fetchEscaneo } from "@/data/escaneos";

import type {
  EjecucionHistorial,
  EscaneoResultResponse,
  HostCve,
  HostDesglose,
  HostVulnerabilidad,
  PuertoResultadoResponse,
} from "@/data/types";

interface Props {
  auditoriaId: string;

  escaneos:
    EjecucionHistorial[];

  data:
    HostDesglose | null;

  pageSize?: number;
}

interface ActivoAuditoria {
  escaneoId: string;
  activo: EscaneoResultResponse["activos"][number];
}

interface ActivoDetalle {
  puertos:
    PuertoResultadoResponse[];

  escaneoIds: string[];
}

type RankingTipo =
  | "criticas"
  | "riesgo";

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
    ? `${(
        score * 100
      ).toFixed(1)}%`
    : "—";
}

/* ============================================================
 * CVEs asociadas al activo
 * ============================================================ */

function ActivoCves({
  items,
  title,
  pageSize = 4,
}: {
  items: HostCve[];
  title: string;
  pageSize?: number;
}) {
  const [page, setPage] =
    useState(0);

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
    return null;
  }

  return (
    <div className="cve-detail-section">
      <h4>
        {title}
      </h4>

      <div className="host-port-list">
        {visible.map(
          (cve) => (
            <div
              className="host-port-item"
              key={
                cve.cveId
              }
            >
              <div className="host-port-main">
                <strong>
                  {
                    cve.cveId
                  }
                </strong>

                {cve.kev && (
                  <span className="cve-badge cve-badge-kev">
                    KEV
                  </span>
                )}
              </div>

              <div className="host-port-meta">
                <span>
                  CVSS:{" "}
                  <strong>
                    {formatearCvss(
                      cve.cvssScore,
                    )}
                  </strong>
                </span>

                <span>
                  EPSS:{" "}
                  <strong>
                    {formatearEpss(
                      cve.epssScore,
                    )}
                  </strong>
                </span>

                <span>
                  Explotación
                  conocida:{" "}
                  <strong>
                    {cve.kev
                      ? "Sí · CISA KEV"
                      : "No"}
                  </strong>
                </span>
              </div>

              {(cve.cpes ??
                []).length >
                0 && (
                <div className="host-port-cpes">
                  <span>
                    CPE
                    asociado
                  </span>

                  {(
                    cve.cpes ??
                    []
                  ).map(
                    (cpe) => (
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
            </div>
          ),
        )}
      </div>

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
 * Ranking de activos
 * ============================================================ */

function ActivoRanking({
  items,
  emptyMessage,
  pageSize,
  details,
  loadingActivos,
  errors,
  loadDetail,
  tipo,
}: {
  items:
    HostVulnerabilidad[];

  emptyMessage: string;

  pageSize: number;

  details: Record<
    string,
    ActivoDetalle
  >;

  loadingActivos:
    Set<string>;

  errors: Record<
    string,
    string
  >;

  loadDetail: (
    ip: string,
  ) => Promise<void>;

  tipo: RankingTipo;
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
          (
            activo,
            index,
          ) => {
            const key =
              `${activo.ip}-${activo.hostname ?? ""}`;

            const expanded =
              expandedId ===
              key;

            const detail =
              details[
                activo.ip
              ];

            const loading =
              loadingActivos.has(
                activo.ip,
              );

            const error =
              errors[
                activo.ip
              ];

            const cves =
              tipo ===
              "criticas"
                ? activo.cvesCriticas ??
                  []
                : activo.cvesPrioritarias ??
                  [];

            return (
              <li
                key={key}
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
                      key,
                    );

                    void loadDetail(
                      activo.ip,
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
                          activo.ip
                        }
                      </strong>

                      <span className="security-ranking-subtitle">
                        {activo.hostname ??
                          "Sin hostname"}
                      </span>

                      {activo.kev && (
                        <span className="cve-badge cve-badge-kev">
                          KEV
                        </span>
                      )}
                    </div>

                    <div className="security-ranking-preview">
                      <span>
                        CVEs{" "}
                        <strong>
                          {
                            activo.vulnerabilidades
                          }
                        </strong>
                      </span>

                      <span>
                        Críticas{" "}
                        <strong>
                          {
                            activo.vulnerabilidadesCriticas
                          }
                        </strong>
                      </span>

                      <span>
                        CVSS
                        máx.{" "}
                        <strong>
                          {formatearCvss(
                            activo.cvssMaximo,
                          )}
                        </strong>
                      </span>

                      <span>
                        EPSS
                        máx.{" "}
                        <strong>
                          {formatearEpss(
                            activo.epssMaximo,
                          )}
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
                    <dl className="security-detail-grid">
                      <div>
                        <dt>
                          Dirección
                          IP
                        </dt>

                        <dd>
                          {
                            activo.ip
                          }
                        </dd>
                      </div>

                      <div>
                        <dt>
                          Hostname
                        </dt>

                        <dd>
                          {activo.hostname ??
                            "Sin hostname"}
                        </dd>
                      </div>

                      <div>
                        <dt>
                          Vulnerabilidades
                        </dt>

                        <dd>
                          {
                            activo.vulnerabilidades
                          }
                        </dd>
                      </div>

                      <div>
                        <dt>
                          Vulnerabilidades
                          críticas
                        </dt>

                        <dd>
                          {
                            activo.vulnerabilidadesCriticas
                          }
                        </dd>
                      </div>

                      <div>
                        <dt>
                          CVSS
                          máximo
                        </dt>

                        <dd>
                          {formatearCvss(
                            activo.cvssMaximo,
                          )}
                        </dd>
                      </div>

                      <div>
                        <dt>
                          EPSS
                          máximo
                        </dt>

                        <dd>
                          {formatearEpss(
                            activo.epssMaximo,
                          )}
                        </dd>
                      </div>

                      <div>
                        <dt>
                          Explotación
                          conocida
                        </dt>

                        <dd>
                          {activo.kev
                            ? "Sí · contiene CVE en CISA KEV"
                            : "No registrada"}
                        </dd>
                      </div>
                    </dl>

                    <ActivoCves
                      items={
                        cves
                      }
                      title={
                        tipo ===
                        "criticas"
                          ? "Vulnerabilidades críticas asociadas"
                          : "Vulnerabilidades priorizadas"
                      }
                    />

                    {loading &&
                      !detail && (
                        <div className="security-detail-loading">
                          Cargando
                          puertos
                          detectados
                          en el
                          activo...
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
                                activo.ip,
                              );
                            }}
                          >
                            Reintentar
                          </button>
                        </div>
                      )}

                    {detail && (
                      <div className="host-detail-section">
                        <div className="host-detail-heading">
                          <h4>
                            Puertos
                            detectados
                          </h4>

                          <span>
                            {
                              detail
                                .puertos
                                .length
                            }
                          </span>
                        </div>

                        {detail
                          .puertos
                          .length ===
                        0 ? (
                          <p className="cve-detail-empty">
                            No hay
                            puertos
                            registrados
                            para este
                            activo.
                          </p>
                        ) : (
                          <div className="host-port-list">
                            {detail.puertos.map(
                              (
                                puerto,
                              ) => (
                                <div
                                  className="host-port-item"
                                  key={`${activo.ip}-${puerto.numero}-${puerto.protocolo}`}
                                >
                                  <div className="host-port-main">
                                    <strong>
                                      {
                                        puerto.numero
                                      }
                                      /
                                      {
                                        puerto.protocolo
                                      }
                                    </strong>

                                    <span>
                                      {
                                        puerto.estado
                                      }
                                    </span>
                                  </div>

                                  <div className="host-port-meta">
                                    <span>
                                      Servicio:{" "}
                                      <strong>
                                        {puerto.servicio ??
                                          "—"}
                                      </strong>
                                    </span>

                                    <span>
                                      Producto:{" "}
                                      <strong>
                                        {puerto.producto ??
                                          "—"}
                                      </strong>
                                    </span>

                                    <span>
                                      Versión:{" "}
                                      <strong>
                                        {puerto.version ??
                                          "—"}
                                      </strong>
                                    </span>

                                    {puerto.extrainfo && (
                                      <span>
                                        Info:{" "}
                                        <strong>
                                          {
                                            puerto.extrainfo
                                          }
                                        </strong>
                                      </span>
                                    )}
                                  </div>

                                  {(puerto.cpes ??
                                    [])
                                    .length >
                                    0 && (
                                    <div className="host-port-cpes">
                                      <span>
                                        CPE
                                      </span>

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
                                </div>
                              ),
                            )}
                          </div>
                        )}

                        {detail
                          .escaneoIds
                          .length >
                          0 && (
                          <p className="host-detail-observed">
                            Activo
                            observado
                            en{" "}
                            {
                              detail
                                .escaneoIds
                                .length
                            }{" "}
                            {detail
                              .escaneoIds
                              .length ===
                            1
                              ? "escaneo"
                              : "escaneos"}
                            .
                          </p>
                        )}
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

function ActivosAuditoria({
  items,
}: {
  items: ActivoAuditoria[];
}) {
  const [
    expandedId,
    setExpandedId,
  ] = useState<string | null>(
    null,
  );

  if (items.length === 0) {
    return (
      <div className="dashboard-empty">
        <p>
          No hay activos registrados
          en la auditoría.
        </p>
      </div>
    );
  }

  return (
    <div className="host-port-list">
      {items.map(
        ({
          activo,
          escaneoId,
        }) => {
          const key =
            `${escaneoId}-${activo.activoId}`;

          const expanded =
            expandedId === key;

          return (
            <div
              key={key}
              className={`host-port-item ${
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
                onClick={() =>
                  setExpandedId(
                    expanded
                      ? null
                      : key,
                  )
                }
              >
                <div className="security-ranking-summary">
                  <div className="security-ranking-title">
                    <strong>
                      {
                        activo.host
                      }
                    </strong>

                    <span className="security-ranking-subtitle">
                      {activo.hostname ??
                        "Sin hostname"}
                    </span>
                  </div>

                  <div className="security-ranking-preview">
                    <span>
                      Puertos{" "}
                      <strong>
                        {
                          activo
                            .puertos
                            .length
                        }
                      </strong>
                    </span>

                    <span>
                      SO{" "}
                      <strong>
                        {activo.so ??
                          "—"}
                      </strong>
                    </span>

                    <span>
                      Probabilidad SO{" "}
                      <strong>
                        {activo.soProbab !=
                        null
                          ? `${activo.soProbab}%`
                          : "—"}
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
                  <dl className="security-detail-grid">
                    <div>
                      <dt>
                        Dirección IP
                      </dt>

                      <dd>
                        {
                          activo.host
                        }
                      </dd>
                    </div>

                    <div>
                      <dt>
                        Hostname
                      </dt>

                      <dd>
                        {activo.hostname ??
                          "Sin hostname"}
                      </dd>
                    </div>

                    <div>
                      <dt>
                        Sistema
                        operativo
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

                    <div>
                      <dt>
                        ID de escaneo
                      </dt>

                      <dd>
                        {
                          escaneoId
                        }
                      </dd>
                    </div>
                  </dl>

                  <div className="host-detail-section">
                    <div className="host-detail-heading">
                      <h4>
                        Puertos
                        detectados
                      </h4>

                      <span>
                        {
                          activo
                            .puertos
                            .length
                        }
                      </span>
                    </div>

                    {activo.puertos
                      .length ===
                    0 ? (
                      <p className="cve-detail-empty">
                        No hay
                        puertos
                        registrados
                        para este
                        activo.
                      </p>
                    ) : (
                      <div className="host-port-list">
                        {activo.puertos.map(
                          (
                            puerto,
                          ) => (
                            <div
                              className="host-port-item"
                              key={`${escaneoId}-${activo.activoId}-${puerto.numero}-${puerto.protocolo}`}
                            >
                              <div className="host-port-main">
                                <strong>
                                  {
                                    puerto.numero
                                  }
                                  /
                                  {
                                    puerto.protocolo
                                  }
                                </strong>

                                <span>
                                  {
                                    puerto.estado
                                  }
                                </span>
                              </div>

                              <div className="host-port-meta">
                                <span>
                                  Servicio:{" "}
                                  <strong>
                                    {puerto.servicio ??
                                      "—"}
                                  </strong>
                                </span>

                                <span>
                                  Producto:{" "}
                                  <strong>
                                    {puerto.producto ??
                                      "—"}
                                  </strong>
                                </span>

                                <span>
                                  Versión:{" "}
                                  <strong>
                                    {puerto.version ??
                                      "—"}
                                  </strong>
                                </span>

                                {puerto.extrainfo && (
                                  <span>
                                    Info:{" "}
                                    <strong>
                                      {
                                        puerto.extrainfo
                                      }
                                    </strong>
                                  </span>
                                )}
                              </div>

                              {(puerto.cpes ??
                                [])
                                .length >
                                0 && (
                                <div className="host-port-cpes">
                                  <span>
                                    CPE
                                  </span>

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
                            </div>
                          ),
                        )}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          );
        },
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

export function ActivoDesglose({
  auditoriaId,
  escaneos,
  data,
  pageSize = 5,
}: Props) {

  const [
    activosAuditoria,
    setActivosAuditoria,
  ] = useState<
    ActivoAuditoria[]
  >([]);

  const [
    loadingActivosAuditoria,
    setLoadingActivosAuditoria,
  ] = useState(false);

  const [
    errorActivosAuditoria,
    setErrorActivosAuditoria,
  ] = useState<
    string | null
  >(null);

  const scanResultCache =
    useRef<
      Map<
        string,
        EscaneoResultResponse
      >
    >(new Map());

  const scanRequestCache =
    useRef<
      Map<
        string,
        Promise<EscaneoResultResponse>
      >
    >(new Map());

  const [
    activoDetails,
    setActivoDetails,
  ] = useState<
    Record<
      string,
      ActivoDetalle
    >
  >({});

  const [
    loadingActivos,
    setLoadingActivos,
  ] = useState<
    Set<string>
  >(() => new Set());

  const [
    activoErrors,
    setActivoErrors,
  ] = useState<
    Record<
      string,
      string
    >
  >({});

  const activoLoadingRef =
    useRef<
      Set<string>
    >(new Set());

  const getScanResult =
    useCallback(
      async (
        escaneoId: string,
      ): Promise<EscaneoResultResponse> => {
        const cached =
          scanResultCache.current.get(
            escaneoId,
          );

        if (cached) {
          return cached;
        }

        const pending =
          scanRequestCache.current.get(
            escaneoId,
          );

        if (pending) {
          return pending;
        }

        const request =
          fetchEscaneo(
            auditoriaId,
            escaneoId,
          );

        scanRequestCache.current.set(
          escaneoId,
          request,
        );

        try {
          const result =
            await request;

          scanResultCache.current.set(
            escaneoId,
            result,
          );

          return result;
        } finally {
          scanRequestCache.current.delete(
            escaneoId,
          );
        }
      },
      [auditoriaId],
    );

  const escaneosKey =
  escaneos
    .map(
      (escaneo) =>
        `${escaneo.escaneoId}:${escaneo.estado}:${escaneo.fecha}`,
    )
    .join("|");

const escaneosCompletados =
  useMemo(
    () =>
      escaneos
        .filter(
          (escaneo) =>
            escaneo.estado ===
            "COMPLETADO",
        )
        .sort(
          (a, b) =>
            new Date(
              a.fecha,
            ).getTime() -
            new Date(
              b.fecha,
            ).getTime(),
        ),
    [escaneosKey],
  );

  useEffect(() => {
    let cancelled = false;

    async function cargarActivosAuditoria() {
      if (
        activosAuditoria.length === 0
      ) {
        setLoadingActivosAuditoria(
          true,
        );
      }

      setErrorActivosAuditoria(
        null,
      );

      try {
        const resultados =
          await Promise.all(
            escaneosCompletados.map(
              async (escaneo) => ({
                escaneoId:
                  escaneo.escaneoId,

                resultado:
                  await getScanResult(
                    escaneo.escaneoId,
                  ),
              }),
            ),
          );

        if (cancelled) {
          return;
        }

        const activos =
          resultados.flatMap(
            ({
              escaneoId,
              resultado,
            }) =>
              resultado.activos.map(
                (activo) => ({
                  escaneoId,
                  activo,
                }),
              ),
          );

        activos.sort(
          (a, b) => {
            const byHost =
              a.activo.host.localeCompare(
                b.activo.host,
              );

            if (byHost !== 0) {
              return byHost;
            }

            return a.escaneoId.localeCompare(
              b.escaneoId,
            );
          },
        );

        setActivosAuditoria(
          activos,
        );
      } catch (error) {
        if (cancelled) {
          return;
        }

        setErrorActivosAuditoria(
          error instanceof Error
            ? error.message
            : "No se pudieron cargar los activos de la auditoría.",
        );
      } finally {
        if (!cancelled) {
          setLoadingActivosAuditoria(
            false,
          );
        }
      }
    }

    void cargarActivosAuditoria();

    return () => {
      cancelled = true;
    };
  }, [
    escaneosCompletados,
    getScanResult,
  ]);

  const loadActivoDetail =
    useCallback(
      async (
        activoIp: string,
      ) => {
        if (
          activoDetails[
            activoIp
          ] ||
          activoLoadingRef.current.has(
            activoIp,
          )
        ) {
          return;
        }

        activoLoadingRef.current.add(
          activoIp,
        );

        setLoadingActivos(
          (current) => {
            const next =
              new Set(
                current,
              );

            next.add(
              activoIp,
            );

            return next;
          },
        );

        setActivoErrors(
          (current) => {
            const next = {
              ...current,
            };

            delete next[
              activoIp
            ];

            return next;
          },
        );

        try {
          const completados =
            [
              ...escaneos,
            ]
              .filter(
                (
                  escaneo,
                ) =>
                  escaneo.estado ===
                  "COMPLETADO",
              )
              .sort(
                (
                  a,
                  b,
                ) =>
                  new Date(
                    a.fecha,
                  ).getTime() -
                  new Date(
                    b.fecha,
                  ).getTime(),
              );

          const resultados =
            await Promise.all(
              completados.map(
                async (
                  escaneo,
                ) => ({
                  escaneo,

                  resultado:
                    await getScanResult(
                      escaneo.escaneoId,
                    ),
                }),
              ),
            );

          const puertos =
            new Map<
              string,
              PuertoResultadoResponse
            >();

          const escaneoIds: string[] =
            [];

          for (const {
            escaneo,
            resultado,
          } of resultados) {
            const activo =
              resultado.activos.find(
                (item) =>
                  item.host ===
                  activoIp,
              );

            if (!activo) {
              continue;
            }

            escaneoIds.push(
              escaneo.escaneoId,
            );

            for (const puerto of activo.puertos) {
              const key =
                `${puerto.numero}/${puerto.protocolo}`;

              puertos.set(
                key,
                puerto,
              );
            }
          }

          const puertosOrdenados =
            [
              ...puertos.values(),
            ].sort(
              (a, b) => {
                if (
                  a.numero !==
                  b.numero
                ) {
                  return (
                    a.numero -
                    b.numero
                  );
                }

                return a.protocolo.localeCompare(
                  b.protocolo,
                );
              },
            );

          setActivoDetails(
            (current) => ({
              ...current,

              [activoIp]: {
                puertos:
                  puertosOrdenados,

                escaneoIds:
                  Array.from(
                    new Set(
                      escaneoIds,
                    ),
                  ),
              },
            }),
          );
        } catch (error) {
          setActivoErrors(
            (current) => ({
              ...current,

              [activoIp]:
                error instanceof
                Error
                  ? error.message
                  : "No se pudieron cargar los puertos del activo.",
            }),
          );
        } finally {
          activoLoadingRef.current.delete(
            activoIp,
          );

          setLoadingActivos(
            (current) => {
              const next =
                new Set(
                  current,
                );

              next.delete(
                activoIp,
              );

              return next;
            },
          );
        }
      },
      [
        escaneos,
        getScanResult,
        activoDetails,
      ],
    );

  return (
    <>
      <div className="security-breakdown-subheading">
        <p className="dashboard-section-eyebrow">
          Activos
        </p>

        <h2 id="activo-breakdown-title">
          Desglose de activos
        </h2>

        <p>
          Activos priorizados
          según las
          vulnerabilidades
          asociadas a los CPE
          detectados en sus
          servicios.
        </p>
      </div>

      <div className="security-breakdown-grid security-breakdown-host-grid">
        <article className="dashboard-card security-breakdown-card activo-desglose-full">

          {loadingActivosAuditoria &&
            activosAuditoria.length ===
              0 && (
              <div className="security-detail-loading">
                Cargando activos de la
                auditoría...
              </div>
            )}

          {errorActivosAuditoria && (
            <div className="security-detail-error">
              <span>
                {errorActivosAuditoria}
              </span>
            </div>
          )}

          {activosAuditoria.length >
            0 && (
            <ActivosAuditoria
              items={
                activosAuditoria
              }
            />
          )}

        </article>
        <article className="dashboard-card security-breakdown-card">
          <h3>
            Más vulnerabilidades
            críticas
          </h3>

          <ActivoRanking
            items={
              data?.masVulnerabilidadesCriticas ??
              []
            }
            tipo="criticas"
            emptyMessage="No hay activos con vulnerabilidades críticas."
            pageSize={
              pageSize
            }
            details={
              activoDetails
            }
            loadingActivos={
              loadingActivos
            }
            errors={
              activoErrors
            }
            loadDetail={
              loadActivoDetail
            }
          />
        </article>

        <article className="dashboard-card security-breakdown-card">
          <h3>
            Mayor riesgo de
            explotación
          </h3>

          <ActivoRanking
            items={
              data?.mayorRiesgoExplotacion ??
              []
            }
            tipo="riesgo"
            emptyMessage="No hay activos con vulnerabilidades asociadas."
            pageSize={
              pageSize
            }
            details={
              activoDetails
            }
            loadingActivos={
              loadingActivos
            }
            errors={
              activoErrors
            }
            loadDetail={
              loadActivoDetail
            }
          />
        </article>
      </div>
    </>
  );
}