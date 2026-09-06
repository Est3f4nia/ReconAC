import type {
  HostDesglose,
  HostVulnerabilidad,
} from "@/data/types";

interface Props {
  data: HostDesglose | null;
}

function HostList({
  items,
  emptyMessage,
}: {
  items: HostVulnerabilidad[];
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
    <ul className="host-breakdown-list">
      {items.map((host) => (
        <li key={`${host.ip}-${host.hostname ?? ""}`}>
          <div className="host-breakdown-main">
            <strong>{host.ip}</strong>

            <span>
              {host.hostname ?? "Sin hostname"}
            </span>
          </div>

          <div className="host-breakdown-metrics">
            <span>
              CVEs: {host.vulnerabilidades}
            </span>

            <span>
              Críticos:{" "}
              {host.vulnerabilidadesCriticas}
            </span>

            <span>
              CVSS máximo:{" "}
              {host.cvssMaximo != null
                ? host.cvssMaximo.toFixed(1)
                : "—"}
            </span>

            <span>
              EPSS máximo:{" "}
              {host.epssMaximo != null
                ? `${(
                    host.epssMaximo * 100
                  ).toFixed(1)}%`
                : "—"}
            </span>
          </div>
        </li>
      ))}
    </ul>
  );
}

export function HostBreakdown({ data }: Props) {
  return (
    <section
      className="dashboard-section"
      aria-labelledby="host-breakdown-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Activos
        </p>

        <h2 id="host-breakdown-title">
          Desglose de hosts
        </h2>

        <p>
          Priorización de hosts según vulnerabilidades
          detectadas.
        </p>
      </div>

      <div className="host-breakdown-grid">
        <article className="dashboard-card">
          <h3>
            Más vulnerabilidades críticas
          </h3>

          <HostList
            items={
              data?.masVulnerabilidadesCriticas ??
              []
            }
            emptyMessage="Todavía no hay datos."
          />
        </article>

        <article className="dashboard-card">
          <h3>
            Mayor probabilidad de explotación
          </h3>

          <HostList
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