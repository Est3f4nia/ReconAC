package com.tup.reconac.feature.activo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;

import com.tup.reconac.feature.activo.dtos.response.ActivoAgrupadoResponse;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;
import com.tup.reconac.feature.activo.services.interfaces.IActivoGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Activos")
@RestController
@RequestMapping("/api/activos")
@RequiredArgsConstructor
public class ActivoGetController {

    private final IActivoGetService activoGet;

    @Operation(summary = "Listar activos agrupados", description = "Página de activos deduplicados con sus IDs de escaneo.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @GetMapping
    public ResponseEntity<Page<ActivoAgrupadoResponse>> findAll(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(activoGet.getAll(pageable));
    }

    @Operation(summary = "Listar activos de un escaneo", description = "Página de activos asociados al escaneo indicado.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @GetMapping("/by-escaneo/{escaneoId}")
    public ResponseEntity<Page<ActivoResponse>> findByEscaneoId(
            @PathVariable UUID escaneoId, @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(activoGet.getByEscaneoId(escaneoId, pageable));
    }
}
