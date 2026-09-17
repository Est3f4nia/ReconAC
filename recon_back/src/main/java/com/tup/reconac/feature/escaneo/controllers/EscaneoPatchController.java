package com.tup.reconac.feature.escaneo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.config.server.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.request.MoverEscaneoRequest;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoPatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Escaneos")
@RestController
@RequestMapping("/api/escaneos")
@RequiredArgsConstructor
public class EscaneoPatchController {

    private final IEscaneoPatchService escaneoPatch;

    @Operation(summary = "Mover un escaneo a otra auditoría", description = "auditoriaId en el cuerpo identifica la auditoría de destino.")
    @ApiResponse(responseCode = "200", description = "Operación completada", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @PatchMapping("/{escaneoId}")
    public ResponseEntity<BaseResponse<Void>> mover(
            @PathVariable UUID escaneoId,
            @Valid @RequestBody MoverEscaneoRequest request
    ) {

        escaneoPatch.mover(
                escaneoId,
                request.auditoriaId()
        );

        return ResponseEntity.ok(
                BaseResponse.ok(
                        null,
                        "Escaneo actualizado"
                )
        );
    }
}
