package com.tup.reconac.feature.auditoria.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaEstadisticasResponse;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Auditorías")
@RestController
@RequestMapping("/api/auditorias")
@RequiredArgsConstructor
public class AuditoriaGetController {

    private final IAuditoriaGetService auditoriaGet;

    @Operation(summary = "Listar auditorías", description = "Página de auditorías del usuario autenticado.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @GetMapping
    public ResponseEntity<Page<AuditoriaResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(auditoriaGet.getAll(pageable));
    }

    @Operation(summary = "Consultar una auditoría", description = "Detalle de una auditoría accesible para el usuario.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditoriaGet.getById(id));
    }

    @Operation(summary = "Consultar estadísticas de escaneos", description = "Totales por estado de los escaneos de la auditoría.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @GetMapping("/{id}/estadisticas")
    public ResponseEntity<AuditoriaEstadisticasResponse> getEstadisticas(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(auditoriaGet.getEstadisticas(id));
    }
}