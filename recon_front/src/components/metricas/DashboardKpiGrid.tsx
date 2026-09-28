import type { DashboardKpis } from "@/data/types";
import "./styles/KpiGrid.css"

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
      label: "CVE",
      value: data?.cves ?? "—",
    }, 
    {
      label: "CVE críticas",
      value: data?.cvesCriticos ?? "—",
    },
    {
      label: "CVSS promedio",
      value: data?.cvssPromedio != null ? data.cvssPromedio.toFixed(2) : "—",
    },
    {
      label: "En CISA KEV",
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
      className="dashboard-section dashboard-summary"
      aria-labelledby="dashboard-kpis-title"
    >
      <div className="dashboard-section-heading">
        <p className="dashboard-section-eyebrow">Métricas</p>
        <h2 id="dashboard-kpis-title">Resumen</h2>
        <p>“—” indica un dato no disponible.</p>
      </div>
      <div className="dashboard-kpi-grid">
        {kpis.map((kpi) => (
          <article className="dashboard-kpi" key={kpi.label}>
            <span>{kpi.label}</span>
            <strong>{kpi.value}</strong>
          </article>
        ))}
      </div>
    </section>
  );
}
