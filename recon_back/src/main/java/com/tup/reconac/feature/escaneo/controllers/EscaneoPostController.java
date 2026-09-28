package com.tup.reconac.feature.escaneo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStartRequest;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoCreateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Escaneos")
@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/escaneos")  // Endpoint iniciar escaneo
@RequiredArgsConstructor
public class EscaneoPostController {

    private final IEscaneoCreateService escaneoPost;

    @Operation(summary = "Iniciar un escaneo", description = "Crea el escaneo y solicita su ejecución asíncrona en Python. Consultar después status y logs. El 201 no implica que el reconocimiento haya finalizado.")
    @ApiResponse(responseCode = "201", description = "Creado", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @PostMapping
    public ResponseEntity<BaseResponse<EscaneoResponse>> startScan(
            @PathVariable UUID auditoriaId,
            @Valid @RequestBody ScanStartRequest req) {
        EscaneoResponse response = escaneoPost.startScan(auditoriaId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                BaseResponse.ok(response, "Escaneo iniciado")
        );
    }
}