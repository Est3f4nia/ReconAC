package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.services.domain.EscaneoConsultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/escaneos")
@RequiredArgsConstructor
public class EscaneoDeleteController {

    private final EscaneoConsultService consult;

    @DeleteMapping("/{escaneoId}")
    public ResponseEntity<BaseResponse<Void>> delete(
            @PathVariable UUID auditoriaId,
            @PathVariable UUID escaneoId) {
        consult.delete(auditoriaId, escaneoId);
        return ResponseEntity.ok(BaseResponse.ok(null, "Escaneo eliminado"));
    }
}
