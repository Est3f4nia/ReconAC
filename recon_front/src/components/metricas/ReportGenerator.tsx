interface Props {
  auditoriaId: string;
}

export function ReportGenerator({
  auditoriaId,
}: Props) {
  function generarMarkdown() {
    console.log(
      "Generar informe Markdown:",
      auditoriaId,
    );
  }

  function generarCsv() {
    console.log(
      "Generar informe CSV:",
      auditoriaId,
    );
  }

  return (
    <section
      className="dashboard-section"
      aria-labelledby="report-generator-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Exportación
        </p>

        <h2 id="report-generator-title">
          Generar informe
        </h2>

        <p>
          Exportá los resultados y métricas de la
          auditoría para su análisis posterior.
        </p>
      </div>

      <div className="report-actions">
        <button
          className="button"
          type="button"
          onClick={generarMarkdown}
        >
          Exportar Markdown
        </button>

        <button
          className="button"
          type="button"
          onClick={generarCsv}
        >
          Exportar CSV
        </button>
      </div>
    </section>
  );
}