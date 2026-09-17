package com.tup.reconac.feature.escaneo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.server.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.response.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.internal.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Integración interna")
@RestController
@RequestMapping("/api/internal/scans")
@RequiredArgsConstructor
public class ScanInternalController {

    private final IEscaneoPostService escaneoPost;

    @Operation(summary = "Recibir progreso desde Python", description = "Uso interno, sin JWT ni CSRF en la configuración actual. jobId es el identificador del trabajo Python (moduloJobId), distinto del ID persistido del escaneo.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", ref = "#/components/responses/Error400")
    @PostMapping("/{jobId}/status")
    public ResponseEntity<BaseResponse<Void>> receiveStatus(
            @PathVariable String jobId,
            @RequestBody ScanStatusResponse status) {
        escaneoPost.updateStatusFromExternal(jobId, status);
        return ResponseEntity.ok(BaseResponse.ok(null, "Estado recibido"));
    }

    @Operation(summary = "Recibir resultado desde Python", description = "Uso interno, sin JWT ni CSRF. jobId identifica el trabajo Python. El cuerpo contiene hosts y apiResults indexado por CPE.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @PostMapping("/{jobId}/callback")
    public ResponseEntity<BaseResponse<Void>> receiveCallback(
            @PathVariable String jobId,
            @RequestBody EscaneoResult result) {
        escaneoPost.processCallback(jobId, result);
        return ResponseEntity.ok(BaseResponse.ok(null, "Resultado recibido"));
    }
}
