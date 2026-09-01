import { Link } from "react-router-dom";
import type { EscaneoResumen } from "@/data/escaneos";
import "./AuditCard.css";

interface AuditCardProps {
  audit: EscaneoResumen;
}

export function AuditCard({ audit }: AuditCardProps) {
  const fecha = audit.ultimoEscaneo
    ? new Date(audit.ultimoEscaneo).toLocaleString("es-AR", {
        day: "2-digit",
        month: "2-digit",
        year: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      })
    : "—";

  return (
    <article className="audit-card">
      <h3 className="audit-card-title">{audit.auditoriaNombre}</h3>

      <hr className="audit-card-divider" />

      <div className="audit-card-info">
        <p>{audit.activos} activos</p>
        <p>{audit.puertos} puertos</p>

        <div className="audit-card-scan">
          <p>Último escaneo:</p>
          <p>{fecha}</p>
        </div>

        <div className="audit-card-status">
          <p>Status:</p>
          <span className={`status status-${audit.status}`}>
            {audit.status.replace("_", " ")}
          </span>
        </div>

        <p className="audit-card-cves">
          {audit.cve} CVE — {audit.cveCriticos} críticos
        </p>
      </div>

      <hr className="audit-card-divider" />

      {audit.escaneoId ? (
        <Link
          to={`/auditorias/${audit.auditoriaId}/escaneos/${audit.escaneoId}`}
          className="audit-card-button"
        >
          Ver
        </Link>
      ) : (
        <span className="audit-card-button audit-card-button--disabled">
          Ver
        </span>
      )}
    </article>
  );
}
