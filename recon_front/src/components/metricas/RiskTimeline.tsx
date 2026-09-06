import type { RiesgoTemporal } from "@/data/types";

interface Props {
  data: RiesgoTemporal[];
}

export function RiskTimeline({ data }: Props) {
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
          Evolución del riesgo a través de las ejecuciones
          de la auditoría.
        </p>
      </div>

      {data.length === 0 ? (
        <div className="dashboard-empty">
          <span>Sin datos suficientes</span>
          <p>
            El timeline estará disponible cuando existan
            métricas calculadas para las ejecuciones.
          </p>
        </div>
      ) : (
        <div className="risk-timeline">
          {data.map((item) => (
            <article
              className={`risk-timeline-item risk-${item.nivel}`}
              key={item.escaneoId}
            >
              <div className="risk-timeline-point" />

              <div className="risk-timeline-content">
                <span>
                  {new Date(item.fecha).toLocaleString()}
                </span>

                <strong>
                  {item.nivel}
                </strong>

                <p>
                  CVSS promedio:{" "}
                  {item.cvssPromedio != null
                    ? item.cvssPromedio.toFixed(1)
                    : "—"}
                </p>

                <div>
                  <span>
                    CVEs: {item.cves}
                  </span>

                  <span>
                    Críticos: {item.cvesCriticos}
                  </span>

                  <span>
                    Explotados: {item.cvesExplotados}
                  </span>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}