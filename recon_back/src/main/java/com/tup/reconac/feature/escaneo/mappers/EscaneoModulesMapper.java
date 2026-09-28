package com.tup.reconac.feature.escaneo.mappers;

import com.tup.reconac.feature.auditoria.models.AuditoriaReporte;
import com.tup.reconac.feature.escaneo.dtos.internal.ModuleScanRequest;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStartRequest;
import com.tup.reconac.feature.escaneo.dtos.request.ModuleReportRequest;
import com.tup.reconac.feature.escaneo.models.Escaneo;
import tools.jackson.databind.JsonNode;

import java.util.Locale;

public class EscaneoModulesMapper {

    public static ModuleScanRequest toModuleRequest(Escaneo escaneo, ScanStartRequest request) {
        return new ModuleScanRequest(
                escaneo.getModuloJobId(),
                escaneo.getId().toString(),
                request.objetivos(),
                request.timeout() != null ? request.timeout() : 600,
                request.icmpTimeout() != null ? request.icmpTimeout() : 5,
                request.maxCveYears() != null ? request.maxCveYears() : 2,
                request.minCvssScore() != null ? request.minCvssScore() : 0.0
        );
    }

    public static ModuleReportRequest toReportRequest(
            Escaneo escaneo,
            AuditoriaReporte formato,
            JsonNode resultado) {

        return new ModuleReportRequest(
                formato.name().toLowerCase(Locale.ROOT),
                escaneo.getId().toString(),
                resultado
        );
    }
}
