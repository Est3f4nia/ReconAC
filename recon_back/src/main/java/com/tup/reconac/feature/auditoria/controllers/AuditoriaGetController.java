package com.tup.reconac.feature.auditoria.controllers;

import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaEstadisticasResponse;
import com.tup.reconac.feature.auditoria.dtos.response.AuditoriaResponse;
import com.tup.reconac.feature.auditoria.services.interfaces.IAuditoriaGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auditorias")
@RequiredArgsConstructor
public class AuditoriaGetController {

    private final IAuditoriaGetService auditoriaGet;

    @GetMapping
    public ResponseEntity<Page<AuditoriaResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(auditoriaGet.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditoriaGet.getById(id));
    }

    @GetMapping("/{id}/estadisticas")
    public ResponseEntity<AuditoriaEstadisticasResponse> getEstadisticas(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(auditoriaGet.getEstadisticas(id));
    }
}