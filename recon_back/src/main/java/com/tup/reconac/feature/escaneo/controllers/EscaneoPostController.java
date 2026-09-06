package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.ScanStartRequest;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/escaneos")  // Endpoint iniciar escaneo
@RequiredArgsConstructor
public class EscaneoPostController {

    private final IEscaneoPostService escaneoPost;

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