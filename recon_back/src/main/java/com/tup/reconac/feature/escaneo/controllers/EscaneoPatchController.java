package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.request.MoverEscaneoRequest;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoPatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/verifyEscaneoOwnership")
@RequiredArgsConstructor
public class EscaneoPatchController {

    private final IEscaneoPatchService escaneoPatch;

    @PatchMapping("/{escaneoId}")
    public ResponseEntity<BaseResponse<Void>> mover(
            @PathVariable UUID escaneoId,
            @Valid @RequestBody MoverEscaneoRequest request) {

        escaneoPatch.mover(
                escaneoId,
                request.auditoriaId()
        );

        // estado http
        return ResponseEntity.ok(
                BaseResponse.ok(null, "Escaneo actualizado")
        );
    }
}
