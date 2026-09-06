import { EjecucionHistorial } from "@/data/types";

interface Props {
  escaneos: EjecucionHistorial[];
}

export function ScanHistory({ escaneos }: Props) {
  const recientes = escaneos.slice(0, 5);

  return (
    <section className="dashboard-section">
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Trazabilidad
        </p>

        <h2>Historial de escaneos</h2>
      </div>

      <div className="scan-history">
        {recientes.map((escaneo) => (
          <article
            className="scan-history-item"
            key={escaneo.escaneoId}
          >
            <div>
              <strong>
                {new Date(
                  escaneo.fecha,
                ).toLocaleString()}
              </strong>

              <span>
                {escaneo.escaneoId}
              </span>
            </div>

            <span
              className={`status status-${escaneo.estado}`}
            >
              {escaneo.estado.replace("_", " ")}
            </span>

            <div>
              <span>
                Activos: {escaneo.activos}
              </span>

              <span>
                CVEs: {escaneo.cves}
              </span>

              <span>
                Críticos: {escaneo.cvesCriticos}
              </span>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}