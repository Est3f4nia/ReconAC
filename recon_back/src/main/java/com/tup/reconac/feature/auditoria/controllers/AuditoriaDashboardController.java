package com.tup.reconac.feature.auditoria.controllers;

import com.tup.reconac.config.BaseResponse;
import com.tup.reconac.feature.escaneo.dtos.EscaneoResumenDto;
import com.tup.reconac.feature.escaneo.services.EscaneoResumenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditorias")
@RequiredArgsConstructor
public class AuditoriaDashboardController {

    private final EscaneoResumenService resumenService;

    @GetMapping("/resumen")
    public ResponseEntity<BaseResponse<List<EscaneoResumenDto>>> getResumen() {
        List<EscaneoResumenDto> resumen = resumenService.getResumen();
        return ResponseEntity.ok(BaseResponse.ok(resumen, "Resumen de auditorías obtenido"));
    }
}