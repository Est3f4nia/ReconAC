package com.tup.reconac.feature.activo.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.tup.reconac.feature.activo.services.interfaces.IActivoDeleteService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Activos")
@RestController
@RequestMapping("/api/activos")
@AllArgsConstructor
public class ActivoDeleteController {

    private final IActivoDeleteService activoDelete;

    @Operation(summary = "Eliminar un activo", description = "Elimina el activo identificado.")
    @ApiResponse(responseCode = "204", description = "Eliminado; sin cuerpo", content = @io.swagger.v3.oas.annotations.media.Content)
    @ApiResponse(responseCode = "404", ref = "#/components/responses/Error404")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        activoDelete.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
