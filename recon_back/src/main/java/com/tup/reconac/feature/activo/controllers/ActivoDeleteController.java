package com.tup.reconac.feature.activo.controllers;

import com.tup.reconac.feature.activo.services.interfaces.IActivoDeleteService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/activos")
@AllArgsConstructor
public class ActivoDeleteController {

    private final IActivoDeleteService activoDelete;

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        activoDelete.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
