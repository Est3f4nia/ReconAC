import type { AuditoriaResponse } from "@/data/types";

interface Props {
  auditoria: AuditoriaResponse;
}

export function AuditoriaHeader({ auditoria }: Props) {
  return (
    <header className="dashboard-section auditoria-header">
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Auditoría
        </p>

        <h1>{auditoria.nombre}</h1>

        <p>{auditoria.objetivo}</p>
      </div>

      <div className="auditoria-header-meta">
        <span>
          Creada:{" "}
          {new Date(
            auditoria.fechaGeneracion,
          ).toLocaleString()}
        </span>

        {auditoria.fechaFinal && (
          <span>
            Finalizada:{" "}
            {new Date(
              auditoria.fechaFinal,
            ).toLocaleString()}
          </span>
        )}
      </div>
    </header>
  );
}