package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResponse;
import com.tup.reconac.feature.escaneo.dtos.ScanStartRequest;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.services.ScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/scan")
@RequiredArgsConstructor
public class ScanController {

    private final ScanService scanService;

    @PostMapping
    public ResponseEntity<BaseResponse<EscaneoResponse>> startScan(
            @PathVariable UUID auditoriaId,
            @Valid @RequestBody ScanStartRequest req) {
        EscaneoResponse response = scanService.startScan(auditoriaId, req);
        return ResponseEntity.status(201).body(BaseResponse.ok(response, "Escaneo iniciado"));
    }

    @GetMapping("/{escaneoId}/status")
    public ResponseEntity<BaseResponse<EscaneoResponse>> getStatus(
            @PathVariable UUID auditoriaId,
            @PathVariable UUID escaneoId) {
        EscaneoResponse response = scanService.getStatus(escaneoId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Estado del escaneo"));
    }

    @PostMapping("/{escaneoId}/callback")
    public ResponseEntity<BaseResponse<Void>> callback(
            @PathVariable UUID auditoriaId,
            @PathVariable UUID escaneoId,
            @RequestBody ScanStatusResponse status) {
        scanService.updateStatusFromExternal(escaneoId, status);
        return ResponseEntity.ok(BaseResponse.ok(null, "Callback procesado"));
    }
}
