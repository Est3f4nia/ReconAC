import { Link } from "react-router-dom";
import "./AuditCard.css";

interface EscaneoResumen {
  auditoriaId: string | number;
  auditoriaNombre: string;
  escaneoId?: string | number | null;
  completadoA?: string | Date | null;
  status?: string | null;
  activos: number;
  puertos: number;
  cve: number;
  cveCriticos: number;
}

interface AuditCardProps {
  audit: EscaneoResumen;
}

export function AuditCard({ audit }: AuditCardProps) {
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

  return (
    <article className="audit-card">
      <h3 className="audit-card-title">{audit.auditoriaNombre}</h3>

      <hr className="audit-card-divider" />

      <div className="audit-card-info console">
        <p>{audit.activos} activos</p>
        <p>{audit.puertos} puertos</p>

        <div className="audit-card-scan">
          <p>Último escaneo:</p>
          <p>{fecha}</p>
        </div>

        <div className="audit-card-status">
          <p>Status:</p>
          <span className={`status ${statusClass}`}>
            {statusText}
          </span>
        </div>

        <p className="audit-card-cves">
          {audit.cve} CVE — {audit.cveCriticos} críticos
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
