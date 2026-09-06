import type {
  CveDesglose,
  CveResumen,
} from "@/data/types";

interface Props {
  data: CveDesglose | null;
}

function CveList({
  items,
  emptyMessage,
}: {
  items: CveResumen[];
  emptyMessage: string;
}) {
  if (items.length === 0) {
    return (
      <div className="dashboard-empty">
        <p>{emptyMessage}</p>
      </div>
    );
  }

  return (
    <ul className="cve-list">
      {items.map((cve) => (
        <li key={cve.cveId}>
          <div>
            <strong>{cve.cveId}</strong>

            <span>
              Frecuencia: {cve.frecuencia}
            </span>
          </div>

          <div className="cve-metrics">
            <span>
              CVSS:{" "}
              {cve.cvssScore != null
                ? cve.cvssScore.toFixed(1)
                : "—"}
            </span>

            <span>
              EPSS:{" "}
              {cve.epssScore != null
                ? `${(
                    cve.epssScore * 100
                  ).toFixed(1)}%`
                : "—"}
            </span>

            {cve.explotacionActiva && (
              <span className="cve-exploited">
                KEV
              </span>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}

export function CveBreakdown({ data }: Props) {
  return (
    <section
      className="dashboard-section"
      aria-labelledby="cve-breakdown-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Vulnerabilidades
        </p>

        <h2 id="cve-breakdown-title">
          Desglose de CVEs
        </h2>

        <p>
          Vulnerabilidades priorizadas según frecuencia,
          explotación, criticidad y probabilidad.
        </p>
      </div>

      <div className="cve-breakdown-grid">
        <article className="dashboard-card">
          <h3>Más comunes</h3>

          <CveList
            items={data?.masComunes ?? []}
            emptyMessage="Todavía no hay datos."
          />
        </article>

        <article className="dashboard-card">
          <h3>Explotación activa</h3>

          <CveList
            items={
              data?.explotacionActiva ?? []
            }
            emptyMessage="No hay datos de explotación activa."
          />
        </article>

        <article className="dashboard-card">
          <h3>Mayor criticidad</h3>

          <CveList
            items={
              data?.mayorCriticidad ?? []
            }
            emptyMessage="Todavía no hay datos."
          />
        </article>

        <article className="dashboard-card">
          <h3>Mayor probabilidad de explotación</h3>

          <CveList
            items={
              data?.mayorProbabilidadExplotacion ??
              []
            }
            emptyMessage="Todavía no hay datos de EPSS."
          />
        </article>
      </div>
    </section>
  );
}