import { Link } from "react-router-dom";
import "./AuditCard.css"; //ver

export type AuditStatus = "En proceso" | "Completada" | "Failed";  // debe coincidir con los del back

export interface AuditCardData {
  id: string;
  nombre: string;
  activos: number;
  puertos: number;
  ultimoEscaneo: string;
  status: AuditStatus;
  cve: number;
  cveCriticos: number;
}

interface AuditCardProps {
  audit: AuditCardData;
}


export function AuditCard({ audit }: AuditCardProps) {
  return (
    <article className="audit-card">
      <h3 className="audit-card-title">
        {audit.nombre}
      </h3>

      <div className="audit-card-info">
        <p>{audit.activos} activos</p>
        <p>{audit.puertos} puertos</p>

        <div className="audit-card-scan">
          <p>Último escaneo:</p>
          <p>{audit.ultimoEscaneo}</p>
        </div>

        <div className="audit-card-status">
          <p>Status:</p>
          <span
            className={`status status-${audit.status
              .toLowerCase()
              .replace(" ", "-")}`}
          >
            {audit.status}
          </span>
        </div>

        <p className="audit-card-cves">
          {audit.cve} CVE — {audit.cveCriticos} críticos
        </p>
      </div>

      <Link
        to={`/auditorias/${audit.id}`}
        className="audit-card-button"
      >
        Ver
      </Link>
    </article>
  );
}