package com.tup.reconac.modules.vulnEnum.controllers;

import com.tup.reconac.modules.vulnEnum.dtos.DashboardAuditoriaResponse;
import com.tup.reconac.modules.vulnEnum.services.interfaces.IMetricasAuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias")
@RequiredArgsConstructor
public class MetricasAuditoriaController {

    private final IMetricasAuditoriaService metricasAuditoriaService;

    @GetMapping("/{auditoriaId}/dashboard")
    public ResponseEntity<DashboardAuditoriaResponse> getDashboard(
            @PathVariable UUID auditoriaId
    ) {
        return ResponseEntity.ok(
                metricasAuditoriaService.getDashboard(auditoriaId)
        );
    }
}
