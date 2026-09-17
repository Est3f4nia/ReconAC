package com.tup.reconac.feature.auditoria.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.server.BaseResponse;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResumenResponseDto;
import com.tup.reconac.feature.auditoria.services.AuditoriaResumenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller página principal: obtiene los datos de las cards de auditoría
 */

@Tag(name = "Métricas / Dashboard")
@RestController
@RequestMapping("/api/auditorias")
@RequiredArgsConstructor
public class AuditoriaDashboardController {

    private final AuditoriaResumenService resumenService;

    @Operation(summary = "Consultar resumen de auditorías", description = "Datos de las tarjetas del usuario autenticado.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @GetMapping("/resumen")
    public ResponseEntity<BaseResponse<List<AuditoriaResumenResponseDto>>> getResumen() {
        List<AuditoriaResumenResponseDto> resumen = resumenService.getResumen();
        return ResponseEntity.ok(BaseResponse.ok(resumen, "Resumen de auditorías obtenido"));
    }
}