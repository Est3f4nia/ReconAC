import {
  useMemo,
  useState,
} from "react";

import {
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import type {
  EscaneoListado,
  RiesgoTemporal,
} from "@/data/types";

import "@/components/metricas/styles/RiskTimeline.css";

interface Props {
  data: RiesgoTemporal[];
  escaneos?: EscaneoListado[];
  pageSize?: number;
}

interface ChartPoint extends RiesgoTemporal {
  timestamp: number;
}

interface TooltipPayloadItem {
  payload: ChartPoint;
}

interface RiskTooltipProps {
  active?: boolean;
  payload?: TooltipPayloadItem[];
  escaneosPorId: Map<
    string,
    EscaneoListado
  >;
}

function etiquetaNivel(
  nivel: RiesgoTemporal["nivel"],
) {
  switch (nivel) {
    case "CRITICO":
      return "Crítico";

    case "ALTO":
      return "Alto";

    case "MEDIO":
      return "Medio";

    case "BAJO":
      return "Bajo";

    default:
      return "Desconocido";
  }
}

function nivelDesdeCvss(
  score: number | null,
) {
  if (score == null) {
    return "DESCONOCIDO";
  }

  if (score >= 9) {
    return "CRITICO";
  }

  if (score >= 7) {
    return "ALTO";
  }

  if (score >= 4) {
    return "MEDIO";
  }

  return "BAJO";
}

function formatearFecha(
  value: string | null | undefined,
) {
  if (!value) {
    return "—";
  }

  return new Date(
    value,
  ).toLocaleString("es-AR");
}

function formatearFechaEje(
  timestamp: number,
) {
  return new Date(
    timestamp,
  ).toLocaleString(
    "es-AR",
    {
      day: "2-digit",
      month: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    },
  );
}

/* =========================================================
 * Tooltip
 * ========================================================= */

function RiskTooltip({
  active,
  payload,
  escaneosPorId,
}: RiskTooltipProps) {
  if (
    !active ||
    !payload ||
    payload.length === 0
  ) {
    return null;
  }

  const point =
    payload[0].payload;

  if (
    point.cvssPromedio == null
  ) {
    return null;
  }

  const escaneo =
    escaneosPorId.get(
      point.escaneoId,
    );

  return (
    <div className="risk-chart-tooltip">
      <div className="risk-chart-tooltip-header">
        <div>
          <span>Escaneo</span>

          <strong>
            {point.escaneoId.slice(
              0,
              8,
            )}
          </strong>
        </div>

        <span
          className={`risk-chart-tooltip-risk risk-${nivelDesdeCvss(
            point.cvssPromedio,
          ).toLowerCase()}`}
        >
          {etiquetaNivel(
            point.nivel,
          )}
        </span>
      </div>

      <div className="risk-chart-tooltip-date">
        {formatearFecha(
          point.fecha,
        )}
      </div>

      <div className="risk-chart-tooltip-metrics">
        <div>
          <span>CVSS</span>

          <strong>
            {point.cvssPromedio.toFixed(
              1,
            )}
          </strong>
        </div>

        <div>
          <span>CVEs</span>

          <strong>
            {point.cves}
          </strong>
        </div>

        <div>
          <span>Críticas</span>

          <strong>
            {point.cvesCriticos}
          </strong>
        </div>

        <div>
          <span>KEV</span>

          <strong>
            {point.cvesExplotados}
          </strong>
        </div>
      </div>

      {escaneo && (
        <div className="risk-chart-tooltip-detail">
          <div>
            <span>Estado</span>

            <strong>
              {escaneo.estado}
            </strong>
          </div>

          <div>
            <span>Progreso</span>

            <strong>
              {escaneo.progreso}%
            </strong>
          </div>

          <div>
            <span>Nmap</span>

            <strong>
              {escaneo.nmapVersion ??
                "—"}
            </strong>
          </div>

          <div className="risk-chart-tooltip-wide">
            <span>Objetivos</span>

            <strong>
              {escaneo.objetivos
                .length > 0
                ? escaneo.objetivos.join(
                    ", ",
                  )
                : "—"}
            </strong>
          </div>
        </div>
      )}
    </div>
  );
}

/* =========================================================
 * Componente
 * ========================================================= */

export function RiskTimeline({
  data,
  escaneos = [],
  pageSize = 8,
}: Props) {
  const [page, setPage] =
    useState(0);

  const [
    hoveredScanId,
    setHoveredScanId,
  ] = useState<
    string | null
  >(null);

  /*
   * Todos los datos permanecen en el gráfico.
   * Las cards son lo único paginado.
   */
  const ordenados =
    useMemo(
      () =>
        [...data].sort(
          (a, b) =>
            new Date(
              a.fecha,
            ).getTime() -
            new Date(
              b.fecha,
            ).getTime(),
        ),
      [data],
    );

  /*
   * Eje X temporal real.
   */
  const chartData: ChartPoint[] =
    useMemo(
      () =>
        ordenados.map(
          (item) => ({
            ...item,

            timestamp:
              new Date(
                item.fecha,
              ).getTime(),
          }),
        ),
      [ordenados],
    );

  const escaneosPorId =
    useMemo(() => {
      return new Map(
        escaneos.map(
          (escaneo) => [
            escaneo.escaneoId,
            escaneo,
          ],
        ),
      );
    }, [escaneos]);

  const totalPages =
    Math.max(
      1,
      Math.ceil(
        ordenados.length /
          pageSize,
      ),
    );

  const safePage =
    Math.min(
      page,
      totalPages - 1,
    );

  const visible =
    ordenados.slice(
      safePage * pageSize,
      (safePage + 1) *
        pageSize,
    );

  const puntosConCvss =
    chartData.filter(
      (item) =>
        item.cvssPromedio != null,
    );

  return (
    <section
      className="dashboard-section risk-timeline-section"
      aria-labelledby="risk-timeline-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Evolución
        </p>

        <h2 id="risk-timeline-title">
          Nivel general de riesgo
        </h2>

        <p>
          Evolución temporal del
          CVSS promedio y nivel de
          riesgo entre las
          ejecuciones de la
          auditoría.
        </p>
      </div>

      {ordenados.length ===
      0 ? (
        <div className="dashboard-empty">
          <span>
            Sin datos suficientes
          </span>

          <p>
            El gráfico estará
            disponible cuando existan
            ejecuciones con métricas
            calculadas.
          </p>
        </div>
      ) : (
        <div className="risk-chart">
          {/* ================================================
              GRÁFICO
              ================================================ */}

          {puntosConCvss.length ===
          0 ? (
            <div className="dashboard-empty">
              <p>
                No existen
                puntuaciones CVSS
                disponibles.
              </p>
            </div>
          ) : (
            <div className="risk-chart-container">
              <ResponsiveContainer
                width="100%"
                height="100%"
              >
                <LineChart
                  data={chartData}
                  margin={{
                    top: 28,
                    right: 30,
                    bottom: 22,
                    left: 4,
                  }}
                >
                  <XAxis
                    dataKey="timestamp"
                    type="number"
                    scale="time"
                    domain={[
                      "dataMin",
                      "dataMax",
                    ]}
                    tickFormatter={
                      formatearFechaEje
                    }
                    tickLine={false}
                    axisLine={false}
                    minTickGap={55}
                    height={55}
                    className="risk-chart-axis"
                  />

                  <YAxis
                    domain={[0, 10]}
                    ticks={[
                      0,
                      2,
                      4,
                      6,
                      8,
                      10,
                    ]}
                    tickLine={false}
                    axisLine={false}
                    width={38}
                    className="risk-chart-axis"
                  />

                  <Tooltip
                    cursor={false}
                    content={
                      <RiskTooltip
                        escaneosPorId={
                          escaneosPorId
                        }
                      />
                    }
                  />

                  <Line
                    type="monotone"
                    dataKey="cvssPromedio"
                    connectNulls={false}
                    stroke="var(--border-acc)"
                    strokeWidth={2.5}
                    isAnimationActive={false}

                    dot={(props) => {
                      const {
                        cx,
                        cy,
                        payload,
                      } = props as {
                        cx?: number;
                        cy?: number;
                        payload?: ChartPoint;
                      };

                      if (
                        cx == null ||
                        cy == null ||
                        !payload ||
                        payload.cvssPromedio ==
                          null
                      ) {
                        return <g />;
                      }

                      const active =
                        hoveredScanId ===
                        payload.escaneoId;

                      return (
                        <g>
                          {active && (
                            <circle
                              cx={cx}
                              cy={cy}
                              r={10}
                              className="risk-chart-dot-halo"
                            />
                          )}

                          <circle
                            cx={cx}
                            cy={cy}
                            r={active ? 6.5 : 5}
                            className={`risk-chart-dot risk-${nivelDesdeCvss(
                              payload.cvssPromedio,
                            ).toLowerCase()}`}
                          />
                        </g>
                      );
                    }}

                    activeDot={(props) => {
                      const {
                        cx,
                        cy,
                        payload,
                      } = props as {
                        cx?: number;
                        cy?: number;
                        payload?: ChartPoint;
                      };

                      if (
                        cx == null ||
                        cy == null ||
                        !payload
                      ) {
                        return <g />;
                      }

                      return (
                        <g>
                          <circle
                            cx={cx}
                            cy={cy}
                            r={11}
                            className="risk-chart-dot-halo"
                          />

                          <circle
                            cx={cx}
                            cy={cy}
                            r={7}
                            className={`risk-chart-dot risk-${nivelDesdeCvss(
                              payload.cvssPromedio,
                            ).toLowerCase()}`}
                          />
                        </g>
                      );
                    }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          )}

          {/* ================================================
              CARDS PAGINADAS
              ================================================ */}

          <div
            className="risk-chart-events"
            role="region"
            aria-label="Ejecuciones de la auditoría"
          >
            {visible.map(
              (item) => {
                const escaneo =
                  escaneosPorId.get(
                    item.escaneoId,
                  );

                const active =
                  hoveredScanId ===
                  item.escaneoId;

                return (
                  <article
                    key={
                      item.escaneoId
                    }
                    className={`risk-chart-event ${
                      active
                        ? "risk-chart-event-active"
                        : ""
                    }`}
                    tabIndex={0}
                    onMouseEnter={() =>
                      setHoveredScanId(
                        item.escaneoId,
                      )
                    }
                    onMouseLeave={() =>
                      setHoveredScanId(
                        null,
                      )
                    }
                    onFocus={() =>
                      setHoveredScanId(
                        item.escaneoId,
                      )
                    }
                    onBlur={() =>
                      setHoveredScanId(
                        null,
                      )
                    }
                  >
                    <header className="risk-chart-event-header">
                      <div>
                        <strong>
                          {etiquetaNivel(
                            item.nivel,
                          )}
                        </strong>

                        <span>
                          Escaneo{" "}
                          {item.escaneoId.slice(
                            0,
                            8,
                          )}
                        </span>
                      </div>

                      {escaneo && (
                        <span className="risk-chart-event-status">
                          {
                            escaneo.estado
                          }
                        </span>
                      )}
                    </header>

                    <span className="risk-chart-event-date">
                      {formatearFecha(
                        item.fecha,
                      )}
                    </span>

                    {escaneo &&
                      escaneo.objetivos
                        .length > 0 && (
                        <div className="risk-chart-event-target">
                          <span>
                            Objetivo
                          </span>

                          <strong>
                            {escaneo.objetivos.join(
                              ", ",
                            )}
                          </strong>
                        </div>
                      )}

                    {/*
                     * Sin métricas acá.
                     *
                     * CVSS / CVEs /
                     * Críticas / KEV se
                     * muestran únicamente
                     * en el tooltip del
                     * gráfico.
                     */}
                  </article>
                );
              },
            )}
          </div>

          {/* ================================================
              PAGINACIÓN
              ================================================ */}

          {totalPages > 1 && (
            <nav
              className="listado-pagination"
              aria-label="Páginas de ejecuciones"
            >
              <button
                type="button"
                className="button button-page"
                disabled={
                  safePage === 0
                }
                onClick={() =>
                  setPage(
                    Math.max(
                      0,
                      safePage - 1,
                    ),
                  )
                }
              >
                Anterior
              </button>

              <span role="status">
                Página{" "}
                {safePage + 1} de{" "}
                {totalPages} ·{" "}
                {ordenados.length}{" "}
                escaneos
              </span>

              <button
                type="button"
                className="button button-page"
                disabled={
                  safePage ===
                  totalPages - 1
                }
                onClick={() =>
                  setPage(
                    Math.min(
                      totalPages -
                        1,
                      safePage + 1,
                    ),
                  )
                }
              >
                Siguiente
              </button>
            </nav>
          )}
        </div>
      )}
    </section>
  );
}