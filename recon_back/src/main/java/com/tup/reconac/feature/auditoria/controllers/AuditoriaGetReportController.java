package com.tup.reconac.feature.auditoria.controllers;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.feature.auditoria.models.AuditoriaReporte;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaReportService;
import org.springframework.core.io.Resource;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Reportes")
@RestController
@RequestMapping("/api/{auditoriaId}/escaneos/{escaneoId}/reporte")
@AllArgsConstructor
public class AuditoriaGetReportController {

    private final IAuditoriaReportService auditoriaReport;

    @Operation(summary = "Descargar un reporte", description = "Requiere un escaneo COMPLETADO con resultado. MD descarga Markdown; CSV descarga un ZIP con archivos CSV. Se conserva la ruta /api/{auditoriaId}/escaneos/{escaneoId}/reporte.")
    @ApiResponse(responseCode = "200", description = "Archivo descargable", content = {
        @io.swagger.v3.oas.annotations.media.Content(mediaType = "text/markdown", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary")),
        @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/zip", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "binary"))
    })
    @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400")
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
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
