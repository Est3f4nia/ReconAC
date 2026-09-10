import type { RiesgoTemporal } from "@/data/types";
import { useState } from "react";
import "@/components/metricas/styles/RiskTimeline.css";

interface Props {
  data: RiesgoTemporal[];
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

function nivelDesdeCvss(score: number | null) {
  if (score == null) return "DESCONOCIDO";

  if (score >= 9) return "CRITICO";
  if (score >= 7) return "ALTO";
  if (score >= 4) return "MEDIO";

  return "BAJO";
}

export function RiskTimeline({ data }: Props) {
  const [page, setPage] = useState(0);
  const pageSize = 8;
  const ordenados = [...data].sort(
    (a, b) =>
      new Date(a.fecha).getTime() -
      new Date(b.fecha).getTime(),
  );

  const totalPages = Math.max(1, Math.ceil(ordenados.length / pageSize));
  const safePage = Math.min(page, totalPages - 1);
  const visible = ordenados.slice(safePage * pageSize, (safePage + 1) * pageSize);
  const puntos = visible.filter(
    (item) => item.cvssPromedio != null,
  );

  const maxScore = 10;
  const chartWidth = 800;
  const chartHeight = 240;
  const paddingX = 40;
  const paddingY = 25;

  const innerWidth = chartWidth - paddingX * 2;
  const innerHeight = chartHeight - paddingY * 2;

  const coordenadas = puntos.map((item, index) => {
    const x =
      puntos.length === 1
        ? chartWidth / 2
        : paddingX +
          (index / (puntos.length - 1)) * innerWidth;

    const y =
      paddingY +
      innerHeight -
      ((item.cvssPromedio ?? 0) / maxScore) *
        innerHeight;

    return {
      ...item,
      x,
      y,
    };
  });

  const path =
    coordenadas.length > 1
      ? coordenadas
          .map(
            (point, index) =>
              `${index === 0 ? "M" : "L"} ${point.x} ${point.y}`,
          )
          .join(" ")
      : "";

  return (
    <section
      className="dashboard-section"
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
          Evolución del CVSS promedio y nivel de riesgo
          entre ejecuciones.
        </p>
      </div>

      {ordenados.length === 0 ? (
        <div className="dashboard-empty">
          <span>Sin datos suficientes</span>
          <p>
            El gráfico estará disponible cuando existan
            ejecuciones con métricas calculadas.
          </p>
        </div>
      ) : (
        <div className="risk-chart">
          {puntos.length === 0 && <p className="dashboard-empty">Esta página no tiene puntuaciones CVSS disponibles.</p>}
          <div className="risk-chart-plot">
            <svg
              viewBox={`0 0 ${chartWidth} ${chartHeight}`}
              role="img"
              aria-label="Evolución del CVSS promedio"
              preserveAspectRatio="none"
            >
              {[0, 2, 4, 6, 8, 10].map((score) => {
                const y =
                  paddingY +
                  innerHeight -
                  (score / maxScore) * innerHeight;

                return (
                  <g key={score}>
                    <line
                      x1={paddingX}
                      x2={chartWidth - paddingX}
                      y1={y}
                      y2={y}
                      className="risk-chart-grid"
                    />

                    <text
                      x={paddingX - 10}
                      y={y + 4}
                      textAnchor="end"
                      className="risk-chart-label"
                    >
                      {score}
                    </text>
                  </g>
                );
              })}

              {path && (
                <path
                  d={path}
                  className="risk-chart-line"
                  fill="none"
                />
              )}

              {coordenadas.map((point) => (
                <g key={point.escaneoId}>
                  <circle
                    cx={point.x}
                    cy={point.y}
                    r="6"
                    className={`risk-chart-point risk-${nivelDesdeCvss(
                      point.cvssPromedio,
                    ).toLowerCase()}`}
                  />

                  <text
                    x={point.x}
                    y={point.y - 12}
                    textAnchor="middle"
                    className="risk-chart-value"
                  >
                    {point.cvssPromedio?.toFixed(1)}
                  </text>
                </g>
              ))}
            </svg>
          </div>

          <div className="risk-chart-events" tabIndex={0} role="region" aria-label="Escaneos de la página">
            {visible.map((item) => (
              <article
                key={item.escaneoId}
                className="risk-chart-event"
              >
                <strong>
                  {etiquetaNivel(item.nivel)}
                </strong>
                <span>Escaneo {item.escaneoId.slice(0, 8)}</span>

                <span>
                  {new Date(item.fecha).toLocaleString()}
                </span>

                <div>
                  <span>
                    CVSS:{" "}
                    {item.cvssPromedio != null
                      ? item.cvssPromedio.toFixed(1)
                      : "—"}
                  </span>

                  <span>CVEs: {item.cves}</span>

                  <span>
                    Críticos: {item.cvesCriticos}
                  </span>

                  <span>
                    Explotados: {item.cvesExplotados}
                  </span>
                </div>
              </article>
            ))}
          </div>
          <nav className="listado-pagination" aria-label="Páginas de evolución de riesgo">
            <button type="button" className="button button-page" disabled={safePage === 0}
              onClick={() => setPage(Math.max(0, safePage - 1))}>Anterior</button>
            <span role="status">Página {safePage + 1} de {totalPages} · {ordenados.length} escaneos</span>
            <button type="button" className="button button-page" disabled={safePage === totalPages - 1}
              onClick={() => setPage(Math.min(totalPages - 1, safePage + 1))}>Siguiente</button>
          </nav>
        </div>
      )}
    </section>
  );
}
