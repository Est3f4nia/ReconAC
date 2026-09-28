import { Link } from "react-router-dom";
import { AuditoriaResumen } from "@/data/auditorias";
import "./AuditCard.css";

interface AuditCardProps {
  audit: AuditoriaResumen;
}

export function AuditCard({ audit }: AuditCardProps) {
  const activos = audit.activos ?? 0;
  const puertos = audit.puertos ?? 0;
  const cve = audit.cve ?? 0;
  const cveCriticos = audit.cveCriticos ?? 0;

  const fecha = audit.completadoA
    ? new Date(audit.completadoA).toLocaleString("es-AR", {
        day: "2-digit",
        month: "2-digit",
        year: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "—";

  const statusText = audit.status
    ? audit.status.replace("_", " ")
    : "SIN ESCANEOS";

  const statusClass = audit.status
    ? `status-${audit.status}`
    : "status-sin-escaneos";
  
  const pluralize = (count: number, singular: string, plural: string): string =>
    `${count} ${count === 1 ? singular : plural}`;

  return (
    <article className="audit-card">
      <h3 className="audit-card-title">{audit.auditoriaNombre}</h3>

      <hr className="audit-card-divider" />

      <div className="audit-card-info console card-audit-console">
        <p>{pluralize(activos, "activo", "activos")}</p>
        <p>{pluralize(puertos, "puerto", "puertos")}</p>

        <div className="audit-card-scan">
          <p>Último escaneo:</p>
          <p>{fecha}</p>
        </div>

        <div className="audit-card-status">
          <p>Estado:</p>
          <span className={`status ${statusClass}`}>
            {statusText}
          </span>
        </div>

        <p className="audit-card-cves">
          {cve} CVE — {cveCriticos} críticos
        </p>
      </div>

      <hr className="audit-card-divider" />

      <Link
        to={`/auditorias/${audit.auditoriaId}`}
        className="button audit-card-button"
      >
        Ver auditoría
      </Link>
    </article>
  );
}
