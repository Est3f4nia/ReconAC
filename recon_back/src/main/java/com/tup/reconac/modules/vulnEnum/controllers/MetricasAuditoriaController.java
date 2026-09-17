package com.tup.reconac.modules.vulnEnum.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.modules.vulnEnum.dtos.DashboardAuditoriaResponse;
import com.tup.reconac.modules.vulnEnum.services.interfaces.IMetricasAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Métricas / Dashboard")
@RestController
@RequestMapping("/api/auditorias")
@RequiredArgsConstructor
public class MetricasAuditoriaController {

    private final IMetricasAuditoriaService metricasAuditoriaService;

    @Operation(summary = "Consultar dashboard de una auditoría", description = "KPIs, historial, evolución del riesgo y desgloses de hosts/CVE del servicio actual.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @GetMapping("/{auditoriaId}/dashboard")
    public ResponseEntity<DashboardAuditoriaResponse> getDashboard(
            @PathVariable UUID auditoriaId
    ) {
        return ResponseEntity.ok(
                metricasAuditoriaService.getDashboard(auditoriaId)
        );
    }
}
