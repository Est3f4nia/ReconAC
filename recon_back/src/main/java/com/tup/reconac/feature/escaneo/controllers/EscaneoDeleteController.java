package com.tup.reconac.feature.escaneo.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.services.EscaneoDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Falta meter interfaz y comprobaciones en el service
 */

@RestController
@RequestMapping("/api/auditorias/{auditoriaId}/escaneos")
@RequiredArgsConstructor
public class EscaneoDeleteController {

    private final EscaneoDeleteService escaneoDelete;

    @DeleteMapping("/{escaneoId}")
    public ResponseEntity<BaseResponse<Void>> deleteEscaneo(
            @PathVariable UUID escaneoId
    ) {
        escaneoDelete.eliminar(escaneoId);

        return ResponseEntity.ok(
                BaseResponse.ok(
                        null,
                        "Escaneo eliminado correctamente"
                )
        );
    }
}
