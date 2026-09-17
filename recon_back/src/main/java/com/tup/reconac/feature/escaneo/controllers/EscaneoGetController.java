package com.tup.reconac.feature.escaneo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.server.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Escaneos")
@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/escaneos")
@RequiredArgsConstructor
public class EscaneoGetController {

    private final IEscaneoGetService escaneoGet;

    // Acceso a solo el estado para que el front muestre progreso
    @Operation(summary = "Consultar estado y progreso", description = "Consulta el estado persistido; scanId en esta respuesta es el ID del escaneo en PostgreSQL.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @GetMapping("/{escaneoId}/status")
    public ResponseEntity<BaseResponse<ScanStatusResponse>> getStatus(
            @PathVariable UUID auditoriaId,
            @PathVariable UUID escaneoId) {
        ScanStatusResponse response = escaneoGet.getStatus(auditoriaId, escaneoId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Estado del escaneo: "));
    }

    @Operation(summary = "Consultar resultado de reconocimiento", description = "Devuelve activos y puertos persistidos con el estado del escaneo.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @GetMapping("/{escaneoId}/resultado")
    public ResponseEntity<BaseResponse<EscaneoResult.EscaneoResultResponse>> getResultado(
            @PathVariable UUID auditoriaId,
            @PathVariable UUID escaneoId) {
        EscaneoResult.EscaneoResultResponse response = escaneoGet.getResultado(auditoriaId, escaneoId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Resultado del escaneo: "));
    }

}
