package com.tup.reconac.modules.vulnEnum.dtos.metricas;

import com.tup.reconac.feature.escaneo.models.Escaneo;
import com.tup.reconac.modules.vulnEnum.dtos.data.CveData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ScanMetrics(
        UUID escaneoId,
        LocalDateTime fecha,
        String estado,
        Integer progreso,
        List<String> objetivos,
        long activos,
        long puertos,
        long cves,
        long cvesCriticos,
        long cvesAltos,
        long cvesMedios,
        long cvesBajos,
        BigDecimal cvssPromedio,
        long cvesExplotados,
        List<CveData> cvesData
) {

    public static ScanMetrics empty(
            Escaneo escaneo,
            long activos,
            long puertos) {

        return new ScanMetrics(
                escaneo.getId(),
                escaneo.getCreadoA(),
                escaneo.getEstado().name(),
                escaneo.getProgreso(),
                escaneo.getObjetivos() == null
                        ? List.of()
                        : List.of(escaneo.getObjetivos()),
                activos,
                puertos,
                0,
                0,
                0,
                0,
                0,
                null,
                0,
                List.of()
        );
    }
}
