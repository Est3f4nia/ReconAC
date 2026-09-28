package com.tup.reconac.modules.vulnEnum.mappers;

import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.*;
import com.tup.reconac.modules.vulnEnum.dtos.metricas.ScanMetrics;

import java.math.BigDecimal;
import java.util.List;

public class MetricasMapper {

    public static EjecucionHistorialResponse toHistorial(ScanMetrics metric, Escaneo escaneo) {
        return new EjecucionHistorialResponse(
                metric.escaneoId(),
                metric.fecha(),
                metric.estado(),
                metric.progreso(),
                metric.objetivos(),
                metric.activos(),
                metric.puertos(),
                metric.cves(),
                metric.cvesCriticos(),
                metric.cvssPromedio(),
                escaneo != null
                        ? escaneo.getMensajeError()
                        : null
        );
    }

    public static RiesgoTemporalResponse toRiesgoTemporal(ScanMetrics metric) {

        return new RiesgoTemporalResponse(
                metric.escaneoId(),
                metric.fecha(),
                calcularNivelRiesgo(metric.cvssPromedio()),
                metric.cvssPromedio(),
                metric.cves(),
                metric.cvesCriticos(),
                metric.cvesExplotados()
        );
    }

    private static String calcularNivelRiesgo(BigDecimal cvss) {
        if (cvss == null) return "DESCONOCIDO";
        if (cvss.compareTo(BigDecimal.valueOf(9)) >= 0) return "CRITICO";
        if (cvss.compareTo(BigDecimal.valueOf(7)) >= 0) return "ALTO";
        if (cvss.compareTo(BigDecimal.valueOf(4)) >= 0) return "MEDIO";

        return "BAJO";
    }

    public static DashboardAuditoriaResponse emptyDashboard() {
        return new DashboardAuditoriaResponse(
                new DashboardKpisResponse(
                        0, 0, 0, 0, 0, 0, 0, 0,
                        null,
                        0,
                        null
                ),
                List.of(),
                List.of(),
                new CveDesgloseResponse(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ),
                new HostDesgloseResponse(
                        List.of(),
                        List.of(),
                        List.of()
                )
        );
    }
}
