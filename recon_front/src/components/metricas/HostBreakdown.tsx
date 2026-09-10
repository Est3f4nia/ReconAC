import { useState } from "react";
import type {
  HostDesglose,
  HostVulnerabilidad,
} from "@/data/types";

// paginación client-side

interface Props {
  data: HostDesglose | null;
  pageSize?: number;
}

function HostRanking({
  items,
  emptyMessage,
  pageSize = 5,
}: {
  items: HostVulnerabilidad[];
  emptyMessage: string;
  pageSize?: number;
}) {
  const [page, setPage] = useState(0);

  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  const safePage = Math.min(page, totalPages - 1);
  const start = safePage * pageSize;
  const visible = items.slice(start, start + pageSize);

  if (items.length === 0) {
    return (
      <div className="dashboard-empty">
        <p>{emptyMessage}</p>
      </div>
    );
  }

  return (
    <>
      <ol className="host-ranking">
        {visible.map((host, index) => (
          <li
            className="host-ranking-item"
            key={`${host.ip}-${host.hostname ?? ""}`}
          >
            <div className="host-ranking-position">
              #{start + index + 1}
            </div>

            <div className="host-ranking-content">
              <div className="host-breakdown-main">
                <strong>{host.ip}</strong>
                <span>{host.hostname ?? "Sin hostname"}</span>
              </div>

              <div className="host-breakdown-metrics">
                <span>CVEs: {host.vulnerabilidades}</span>
                <span>
                  Críticos: {host.vulnerabilidadesCriticas}
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
                    ? `${(host.epssMaximo * 100).toFixed(1)}%`
                    : "—"}
                </span>
              </div>
            </div>
          </li>
        ))}
      </ol>

      {items.length > pageSize && (
        <div className="listado-pagination host-ranking-pagination">
          <button
            type="button"
            className="button"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={safePage === 0}
          >
            Anterior
          </button>
          <span>
            Página {safePage + 1} de {totalPages}
          </span>
          <button
            type="button"
            className="button"
            onClick={() =>
              setPage((p) => Math.min(totalPages - 1, p + 1))
            }
            disabled={safePage >= totalPages - 1}
          >
            Siguiente
          </button>
        </div>
      )}
    </>
  );
}

export function HostBreakdown({ data, pageSize = 5 }: Props) {
  return (
    <section
      className="dashboard-section"
      aria-labelledby="host-breakdown-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">Activos</p>
        <h2 id="host-breakdown-title">Desglose de hosts</h2>
        <p>
          Hosts priorizados según las vulnerabilidades detectadas
          durante las ejecuciones.
        </p>
      </div>

      <div className="host-breakdown-grid">
        <article className="dashboard-card">
          <h3>Más vulnerabilidades críticas</h3>
          <HostRanking
            items={data?.masVulnerabilidadesCriticas ?? []}
            emptyMessage="Todavía no hay datos."
            pageSize={pageSize}
          />
        </article>

        <article className="dashboard-card">
          <h3>Mayor probabilidad de explotación</h3>
          <HostRanking
            items={data?.mayorProbabilidadExplotacion ?? []}
            emptyMessage="Todavía no hay datos de EPSS."
            pageSize={pageSize}
          />
        </article>
      </div>
    </section>
  );
}