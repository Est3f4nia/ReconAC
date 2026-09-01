package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResultResponse;
import com.tup.reconac.feature.escaneo.dtos.ScanStatusResponse;
import com.tup.reconac.feature.escaneo.services.interfaces.IEscaneoGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/escaneos")
@RequiredArgsConstructor
public class EscaneoGetController {

    private final IEscaneoGetService escaneoGet;

    // Acceso a solo el estado para que el front muestre progreso
    @GetMapping("/{escaneoId}/status")
    public ResponseEntity<BaseResponse<ScanStatusResponse>> getStatus(
            @PathVariable UUID auditoriaId,
            @PathVariable UUID escaneoId) {
        ScanStatusResponse response = escaneoGet.getStatus(auditoriaId, escaneoId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Estado del escaneo: "));
    }

    @GetMapping("/{escaneoId}/resultado")
    public ResponseEntity<BaseResponse<EscaneoResultResponse>> getResultado(
            @PathVariable UUID auditoriaId,
            @PathVariable UUID escaneoId) {
        EscaneoResultResponse response = escaneoGet.getResultado(auditoriaId, escaneoId);
        return ResponseEntity.ok(BaseResponse.ok(response, "Resultado del escaneo: "));
    }

}
