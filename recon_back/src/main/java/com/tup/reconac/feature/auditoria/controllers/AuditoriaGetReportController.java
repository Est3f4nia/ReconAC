package com.tup.reconac.feature.auditoria.controllers;


import com.tup.reconac.feature.auditoria.models.AuditoriaReporte;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaReportService;
import org.springframework.core.io.Resource;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/{auditoriaId}/escaneos/{escaneoId}/reporte")
@AllArgsConstructor
public class AuditoriaGetReportController {

    private final IAuditoriaReportService auditoriaReport;

    @GetMapping
    public ResponseEntity<Resource> generarReporte(
            @PathVariable UUID auditoriaId,
            @PathVariable UUID escaneoId,
            @RequestParam AuditoriaReporte formato
    ) {
        return auditoriaReport.generar(
                auditoriaId,
                escaneoId,
                formato
        );
    }
}
