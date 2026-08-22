package com.tup.reconac.feature.activo.controllers;

import com.tup.reconac.feature.activo.dtos.ActivoResponse;
import com.tup.reconac.feature.activo.services.interfaces.IActivoGetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activos")
@RequiredArgsConstructor
public class ActivoGetController {

    private final IActivoGetService activoGet;

    @GetMapping("/all")
    public ResponseEntity<List<ActivoResponse>> findAll() {
        return ResponseEntity.ok(activoGet.getAll());
    }
}
