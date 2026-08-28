package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResult;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/scans")
@RequiredArgsConstructor
public class ScanInternalController {

    private final IEscaneoPostService escaneoPost;

    @PostMapping("/{jobId}/status")
    public ResponseEntity<BaseResponse<Void>> receiveStatus(
            @PathVariable String jobId,
            @RequestBody ScanStatusResponse status) {
        escaneoPost.updateStatusFromExternal(jobId, status);
        return ResponseEntity.ok(BaseResponse.ok(null, "Estado recibido"));
    }

    @PostMapping("/{jobId}/callback")
    public ResponseEntity<BaseResponse<Void>> receiveCallback(
            @PathVariable String jobId,
            @RequestBody EscaneoResult result) {
        escaneoPost.processCallback(jobId, result);
        return ResponseEntity.ok(BaseResponse.ok(null, "Resultado recibido"));
    }
}
