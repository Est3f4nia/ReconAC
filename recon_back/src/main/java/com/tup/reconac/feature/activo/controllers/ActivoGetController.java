package com.tup.reconac.feature.activo.controllers;

import com.tup.reconac.feature.activo.dtos.response.ActivoAgrupadoResponse;
import com.tup.reconac.feature.activo.dtos.response.ActivoResponse;
import com.tup.reconac.feature.activo.services.interfaces.IActivoGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/activos")
@RequiredArgsConstructor
public class ActivoGetController {

    private final IActivoGetService activoGet;

    @GetMapping
    public ResponseEntity<Page<ActivoAgrupadoResponse>> findAll(Pageable pageable) {
        return ResponseEntity.ok(activoGet.getAll(pageable));
    }

    @GetMapping("/by-escaneo/{escaneoId}")
    public ResponseEntity<Page<ActivoResponse>> findByEscaneoId(
            @PathVariable UUID escaneoId, Pageable pageable) {
        return ResponseEntity.ok(activoGet.getByEscaneoId(escaneoId, pageable));
    }
}
