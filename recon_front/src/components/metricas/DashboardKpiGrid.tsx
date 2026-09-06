import { DashboardKpis } from "@/data/types";

interface Props {
  data: DashboardKpis | null;
}

export function DashboardKpiGrid({ data }: Props) {
  const kpis = [
    {
      label: "Escaneos",
      value: data?.escaneos ?? "—",
    },
    {
      label: "Activos",
      value: data?.activos ?? "—",
    },
    {
      label: "Puertos",
      value: data?.puertos ?? "—",
    },
    {
      label: "CVEs",
      value: data?.cves ?? "—",
    },
    {
      label: "CVEs críticos",
      value: data?.cvesCriticos ?? "—",
    },
    {
      label: "CVSS promedio",
      value:
        data?.cvssPromedio != null
          ? data.cvssPromedio.toFixed(2)
          : "—",
    },
    {
      label: "Explotación activa",
      value: data?.cvesExplotados ?? "—",
    },
    {
      label: "EPSS promedio",
      value:
        data?.epssPromedio != null
          ? `${(data.epssPromedio * 100).toFixed(1)}%`
          : "—",
    },
  ];

  return (
    <section
      className="dashboard-section"
      aria-labelledby="dashboard-kpis-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">
          Proyecto
        </p>

        <h2 id="dashboard-kpis-title">
          Estadísticas generales
        </h2>
      </div>

      <div className="dashboard-kpi-grid">
        {kpis.map((kpi) => (
          <article
            className="dashboard-kpi"
            key={kpi.label}
          >
            <span>{kpi.label}</span>
            <strong>{kpi.value}</strong>
          </article>
        ))}
      </div>
    </section>
  );
}