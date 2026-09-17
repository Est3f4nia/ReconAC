import type {
  CveDesglose as CveDesgloseType,
  EjecucionHistorial,
  HostDesglose,
} from "@/data/types";

import { CveDesglose } from "@/components/metricas/CveDesglose";
import { ActivoDesglose } from "@/components/metricas/ActivoDesglose";

import "@/components/metricas/styles/SecurityBreakdown.css";

interface Props {
  auditoriaId: string;
  escaneos: EjecucionHistorial[];

  cves: CveDesgloseType | null;
  hosts: HostDesglose | null;

  pageSize?: number;
}

export function SecurityBreakdown({
  auditoriaId,
  escaneos,
  cves,
  hosts,
  pageSize = 5,
}: Props) {
  return (
    <>
      <section
        className="dashboard-section security-breakdown"
        aria-labelledby="activo-breakdown-title"
      >
        <ActivoDesglose
          auditoriaId={auditoriaId}
          escaneos={escaneos}
          data={hosts}
          pageSize={pageSize}
        />
      </section>

      <section
        className="dashboard-section security-breakdown"
        aria-labelledby="cve-breakdown-title"
      >
        <CveDesglose
          data={cves}
          pageSize={pageSize}
        />
      </section>
    </>
  );
}