import { useState } from "react";
import type {
  CveDesglose,
  CveResumen,
} from "@/data/types";

interface Props {
  data: CveDesglose | null;
  pageSize?: number;
}

function formatearCvss(score: number | null) {
  return score != null ? score.toFixed(1) : "—";
}

function formatearEpss(score: number | null) {
  return score != null
    ? `${(score * 100).toFixed(1)}%`
    : "—";
}

function CveRanking({
  items,
  emptyMessage,
  pageSize = 5,
}: {
  items: CveResumen[];
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
      <ol className="cve-ranking">
        {visible.map((cve, index) => (
          <li className="cve-ranking-item" key={cve.cveId}>
            <div className="cve-ranking-position">
              #{start + index + 1}
            </div>

            <div className="cve-ranking-content">
              <div className="cve-ranking-header">
                <strong>{cve.cveId}</strong>
                {cve.explotacionActiva && (
                  <span className="cve-badge cve-badge-kev">
                    KEV
                  </span>
                )}
              </div>

              <div className="cve-ranking-main">
                <span>
                  CVSS:{" "}
                  <strong>{formatearCvss(cve.cvssScore)}</strong>
                </span>
                <span>
                  EPSS:{" "}
                  <strong>{formatearEpss(cve.epssScore)}</strong>
                </span>
              </div>

              <div className="cve-ranking-secondary">
                <span>Frecuencia: {cve.frecuencia}</span>
                {cve.cwes.length > 0 && (
                  <span>CWE: {cve.cwes.join(", ")}</span>
                )}
              </div>
            </div>
          </li>
        ))}
      </ol>

      {items.length > pageSize && (
        <div className="listado-pagination cve-ranking-pagination">
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

export function CveBreakdown({ data, pageSize = 5 }: Props) {
  return (
    <section
      className="dashboard-section"
      aria-labelledby="cve-breakdown-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Vulnerabilidades
        </p>
        <h2 id="cve-breakdown-title">Desglose de CVEs</h2>
        <p>
          Vulnerabilidades priorizadas por criticidad,
          probabilidad de explotación y frecuencia.
        </p>
      </div>

      <div className="cve-breakdown-grid">
        <article className="dashboard-card">
          <h3>Mayor criticidad</h3>
          <CveRanking
            items={data?.mayorCriticidad ?? []}
            emptyMessage="Todavía no hay datos."
            pageSize={pageSize}
          />
        </article>

        <article className="dashboard-card">
          <h3>Mayor probabilidad de explotación</h3>
          <CveRanking
            items={data?.mayorProbabilidadExplotacion ?? []}
            emptyMessage="Todavía no hay datos de EPSS."
            pageSize={pageSize}
          />
        </article>

        <article className="dashboard-card">
          <h3>Más frecuentes</h3>
          <CveRanking
            items={data?.masComunes ?? []}
            emptyMessage="Todavía no hay datos."
            pageSize={pageSize}
          />
        </article>

        <article className="dashboard-card">
          <h3>Explotación activa</h3>
          <CveRanking
            items={data?.explotacionActiva ?? []}
            emptyMessage="No hay vulnerabilidades catalogadas como explotación activa."
            pageSize={pageSize}
          />
        </article>
      </div>
    </section>
  );
}